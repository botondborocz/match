import React, { useState, useRef, useEffect, useMemo } from 'react';
import { APIProvider, Map, AdvancedMarker, useMap } from '@vis.gl/react-google-maps';
import { Plus, X, Star, LocateFixed, Navigation } from 'lucide-react';
import { useTranslation } from 'react-i18next';
import './MapScreen.css';
import { SERVER_IP } from '../../constants';
import { useTheme } from '../../theme/ThemeContext';

// --- Types & Mock Data ---
export interface TTClub {
    id: string;
    name: string;
    distance: string;
    tables: number;
    rating: number;
    lat: number;
    lng: number;
    tags: string[];
}

// 👇 1. Set the cutoff point. 12 is roughly "City level" on Google Maps.
const MIN_ZOOM_TO_SHOW_CLUBS = 12;

function MapAnimator({ targetLocation }: { targetLocation: { lat: number, lng: number, zoom: number } | null }) {
    const map = useMap();

    useEffect(() => {
        if (map && targetLocation) {
            map.panTo({ lat: targetLocation.lat, lng: targetLocation.lng });
            setTimeout(() => {
                map.setZoom(targetLocation.zoom);
            }, 300);
        }
    }, [map, targetLocation]);

    return null;
}

type SheetState = 'collapsed' | 'half' | 'expanded';

export default function MapScreen() {
    const { t } = useTranslation();
    const { theme } = useTheme();

    const isDarkMode = useMemo(() => {
        if (theme === 'dark') return true;
        if (theme === 'light') return false;
        // If it's 'system', ask the browser/phone what the OS is currently set to!
        return window.matchMedia('(prefers-color-scheme: dark)').matches;
    }, [theme]);

    console.log(`Current Theme: ${theme} | Dark Mode Active: ${isDarkMode}`);


    // --- State ---
    const [clubs, setClubs] = useState<TTClub[]>([]);
    const [isLoading, setIsLoading] = useState(true);
    const [selectedClub, setSelectedClub] = useState<TTClub | null>(null);
    const [mapCenter, setMapCenter] = useState({ lat: 47.4979, lng: 19.0402 }); // Budapest

    const [panTarget, setPanTarget] = useState<{ lat: number, lng: number, zoom: number } | null>(null);
    const [userLocation, setUserLocation] = useState<{ lat: number, lng: number } | null>(null);
    const [showMapChoice, setShowMapChoice] = useState(false);

    // 👇 2. Track Zoom and Map Bounds (Edges of the screen)
    const [currentZoom, setCurrentZoom] = useState(13); // Matches defaultZoom
    const [mapBounds, setMapBounds] = useState<{
        north: number, south: number, east: number, west: number
    } | null>(null);
    const watchIdRef = useRef<number | null>(null);

    // --- FETCH CLUBS FROM KTOR BACKEND ---
    useEffect(() => {
        const fetchLocations = async () => {
            try {
                // const token = localStorage.getItem('auth_token');
                const response = await fetch(`${SERVER_IP}/api/locations/nearby`, {
                    method: 'GET',
                    headers: {
                        // 'Authorization': `Bearer ${token}`,
                        'Content-Type': 'application/json'
                    }
                });

                if (!response.ok) {
                    throw new Error(`HTTP error! status: ${response.status}`);
                }

                const data = await response.json();
                const mappedClubs: TTClub[] = data.map((loc: any) => ({
                    id: loc.id,
                    name: loc.name,
                    distance: "Distance unknown",
                    tables: loc.tableCount,
                    rating: 0.0,
                    lat: loc.latitude,
                    lng: loc.longitude,
                    tags: [
                        loc.type,
                        loc.isFree ? "Free" : "Paid"
                    ]
                }));

                setClubs(mappedClubs);
            } catch (error) {
                console.error("Failed to fetch nearby locations:", error);
            } finally {
                setIsLoading(false);
            }
        };

        fetchLocations();
    }, []);

    // --- Geolocation Cleanup ---
    // This makes sure we turn off the GPS if the user navigates away from the map
    useEffect(() => {
        return () => {
            if (watchIdRef.current !== null) {
                navigator.geolocation.clearWatch(watchIdRef.current);
            }
        };
    }, []);

    // --- The new "Center Me" Click Handler ---
    const handleCenterMeClick = () => {
        if (!navigator.geolocation) {
            console.error("Geolocation is not supported by your browser");
            return;
        }

        // 1. If we already have their location, just pan immediately!
        if (watchIdRef.current !== null && userLocation) {
            setPanTarget({ lat: userLocation.lat, lng: userLocation.lng, zoom: 16 });
            return;
        }

        // 2. Otherwise, request permission and get their initial location
        navigator.geolocation.getCurrentPosition(
            (position) => {
                const newLoc = {
                    lat: position.coords.latitude,
                    lng: position.coords.longitude
                };
                setUserLocation(newLoc);
                setPanTarget({ lat: newLoc.lat, lng: newLoc.lng, zoom: 16 });

                // 3. Now that we have permission, start "watching" so the blue dot follows them
                if (watchIdRef.current === null) {
                    watchIdRef.current = navigator.geolocation.watchPosition(
                        (pos) => setUserLocation({
                            lat: pos.coords.latitude,
                            lng: pos.coords.longitude
                        }),
                        (err) => console.error("Error watching location:", err),
                        { enableHighAccuracy: true, maximumAge: 0 }
                    );
                }
            },
            (error) => {
                console.error("User denied location or error occurred:", error);
                // Optional: Show a little toast message here telling them to enable permissions in settings
            },
            { enableHighAccuracy: true }
        );
    };

    const [displayClub, setDisplayClub] = useState<TTClub | null>(null);
    const [isExiting, setIsExiting] = useState(false);

    useEffect(() => {
        if (selectedClub) {
            setIsExiting(false);
            setDisplayClub(selectedClub);
            setShowMapChoice(false);
        } else if (displayClub) {
            setIsExiting(true);
            setShowMapChoice(false);
            const timer = setTimeout(() => {
                setDisplayClub(null);
                setIsExiting(false);
            }, 300);
            return () => clearTimeout(timer);
        }
    }, [selectedClub, displayClub]);

    // Filter States
    const [isIndoor, setIsIndoor] = useState(false);
    const [isOutdoor, setIsOutdoor] = useState(true);
    const [isFree, setIsFree] = useState(false);

    // Bottom Sheet States
    const [sheetState, setSheetState] = useState<SheetState>('collapsed');
    const [isDragging, setIsDragging] = useState(false);
    const [dragOffset, setDragOffset] = useState(0);
    const dragStartY = useRef(0);
    const contentRef = useRef<HTMLDivElement>(null);

    // --- The Holy Grail Scroll Interceptor ---
    useEffect(() => {
        const content = contentRef.current;
        if (!content) return;

        let touchStartY = 0;

        const handleTouchStart = (e: TouchEvent) => {
            touchStartY = e.touches[0].clientY;
        };

        const handleTouchMove = (e: TouchEvent) => {
            // We only need to hijack the scroll if the sheet is fully expanded
            if (sheetState !== 'expanded') return;

            const currentY = e.touches[0].clientY;
            const isPullingDown = currentY > touchStartY;

            // If we are at the absolute top of the list AND pulling down...
            // (We use <= 2 to account for weird mobile screen decimal pixels)
            if (content.scrollTop <= 2 && isPullingDown) {
                // Magic Line: Kills the native overscroll/bounce and keeps our React Pointer logic alive!
                e.preventDefault();
            }
        };

        // We MUST set passive: false so the browser allows us to call preventDefault()
        content.addEventListener('touchstart', handleTouchStart, { passive: true });
        content.addEventListener('touchmove', handleTouchMove, { passive: false });

        return () => {
            content.removeEventListener('touchstart', handleTouchStart);
            content.removeEventListener('touchmove', handleTouchMove);
        };
    }, [sheetState]); // Re-run this effect if the sheet state changes

    const handlePointerDown = (e: React.PointerEvent<HTMLDivElement>) => {
        if (window.innerWidth >= 768) return;

        const target = e.target as HTMLElement;
        const content = target.closest('.sheet-content');

        // Traffic Cop Part 1: If we are expanded AND the user has scrolled down the list 
        // even a single pixel, ABORT drag. Let the browser handle the scrolling.
        if (sheetState === 'expanded' && content) {
            // Give a tiny 2px buffer for weird mobile scrolling decimals
            if (contentRef.current && contentRef.current.scrollTop > 2) {
                return;
            }
        }

        setIsDragging(true);
        dragStartY.current = e.clientY - dragOffset;
    };

    const handlePointerMove = (e: React.PointerEvent<HTMLDivElement>) => {
        if (!isDragging) return;

        const currentY = e.clientY - dragStartY.current;

        // Traffic Cop Part 2: If we are expanded, the list is at the very top, 
        // BUT the user's finger is moving UP (currentY < 0)...
        // They are trying to scroll down the list! Cancel the drag and let them scroll.
        if (sheetState === 'expanded' && currentY < 0) {
            setIsDragging(false);
            setDragOffset(0);
            return;
        }

        // If they are pulling down (currentY > 0), OR they are in the half/collapsed state,
        // drag the sheet!
        setDragOffset(currentY);
    };

    const handlePointerUp = () => {
        if (!isDragging) return;
        setIsDragging(false);

        // Tuning parameters (Adjust these to make it feel heavier or lighter!)
        const BIG_SWIPE = 200; // pixels
        const SMALL_SWIPE = 50;  // pixels

        // Remember: dragOffset is NEGATIVE when pulling UP, and POSITIVE when pushing DOWN.

        if (dragOffset < -BIG_SWIPE) {
            // 1. Huge swipe UP -> Skip straight to expanded
            setSheetState('expanded');

        } else if (dragOffset < -SMALL_SWIPE) {
            // 2. Normal swipe UP -> Go up just one level
            setSheetState(sheetState === 'collapsed' ? 'half' : 'expanded');

        } else if (dragOffset > BIG_SWIPE) {
            // 3. Huge swipe DOWN -> Skip straight to collapsed
            setSheetState('collapsed');

        } else if (dragOffset > SMALL_SWIPE) {
            // 4. Normal swipe DOWN -> Go down just one level
            setSheetState(sheetState === 'expanded' ? 'half' : 'collapsed');
        }

        // Reset the drag offset so the CSS transition handles the smooth snapping
        setDragOffset(0);
    };

    const handleClubClick = (club: TTClub) => {
        setSelectedClub(club);
        setSheetState('collapsed');
        setPanTarget({ lat: club.lat, lng: club.lng, zoom: 16 });
    };

    // 👇 3. THE MAGIC FILTER: Automatically updates when map moves
    const visibleClubs = useMemo(() => {
        if (currentZoom < MIN_ZOOM_TO_SHOW_CLUBS || !mapBounds) {
            return []; // Zoomed out too far, or bounds not loaded yet
        }

        return clubs.filter(club => {
            const isInsideLat = club.lat <= mapBounds.north && club.lat >= mapBounds.south;
            const isInsideLng = club.lng <= mapBounds.east && club.lng >= mapBounds.west;
            return isInsideLat && isInsideLng;
        });
    }, [clubs, currentZoom, mapBounds]);

    const map_api_key = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || "";

    return (
        <div className="map-screen-container">
            <APIProvider apiKey={map_api_key}>
                <Map
                    center={mapCenter}
                    // 👇 4. Update the event listener to catch zoom and bounds!
                    onCameraChanged={(ev) => {
                        setMapCenter(ev.detail.center);
                        setCurrentZoom(ev.detail.zoom);
                        setMapBounds(ev.detail.bounds);
                    }}
                    defaultZoom={13}
                    gestureHandling={'greedy'}
                    disableDefaultUI={true}
                    mapId="DEMO_MAP_ID"
                    style={{ width: '100%', height: '100%' }}
                    colorScheme={isDarkMode ? 'DARK' : 'LIGHT'}
                >
                    <MapAnimator targetLocation={panTarget} />

                    {userLocation && (
                        <AdvancedMarker position={userLocation} zIndex={50}>
                            <div className="user-location-marker">
                                <div className="user-location-halo"></div>
                                <div className="user-location-dot"></div>
                            </div>
                        </AdvancedMarker>
                    )}

                    {clubs.map(club => {
                        const isSelected = club.id === selectedClub?.id;
                        return (
                            <AdvancedMarker
                                key={club.id}
                                position={{ lat: club.lat, lng: club.lng }}
                                zIndex={isSelected ? 40 : 10}
                                onClick={() => handleClubClick(club)}
                            >
                                <div className={`club-marker ${isSelected ? 'selected' : ''}`}>
                                    🏓
                                </div>
                            </AdvancedMarker>
                        );
                    })}
                </Map>
            </APIProvider>

            {/* FLOATING FILTER CHIPS */}
            <div className="floating-filters">
                <button className={`filter-chip ${isIndoor ? 'active' : ''}`} onClick={() => setIsIndoor(!isIndoor)}>
                    {t('indoor', 'Indoor')}
                </button>
                <button className={`filter-chip ${isOutdoor ? 'active' : ''}`} onClick={() => setIsOutdoor(!isOutdoor)}>
                    {t('outdoor', 'Outdoor')}
                </button>
                <button className={`filter-chip ${isFree ? 'active' : ''}`} onClick={() => setIsFree(!isFree)}>
                    {t('free', 'Free')}
                </button>
            </div>

            {/* FLOATING ACTION BUTTONS */}
            {(!selectedClub && sheetState !== 'expanded') && (
                <div className={`floating-actions sheet-${sheetState}`}>
                    <button className="fab-btn">
                        <Plus size={24} />
                    </button>
                    <button
                        className="fab-btn"
                        onClick={handleCenterMeClick}
                    >
                        <LocateFixed size={24} />
                    </button>
                </div>
            )}

            {/* SELECTED CLUB CARD */}
            {displayClub && (
                <div
                    key={displayClub.id}
                    className={`selected-club-overlay ${isExiting ? 'exiting' : ''}`}
                >
                    <div className="selected-club-card">
                        <div className="card-header">
                            <div>
                                <h3 className="club-name">{displayClub.name}</h3>
                                <p className="club-subtitle">{displayClub.distance} • {displayClub.tables} Tables</p>
                            </div>
                            <button className="close-btn" onClick={() => setSelectedClub(null)}>
                                <X size={20} />
                            </button>
                        </div>
                        <div className="card-footer" style={{ position: 'relative', justifyContent: 'space-between', display: 'flex', alignItems: 'center', width: '100%' }}>

                            {/* --- LEFT SIDE: Conditional between Rating or Cancel --- */}
                            {showMapChoice ? (
                                // The NEW context-aware Cancel Button (replaces Rating)
                                <button
                                    onClick={() => setShowMapChoice(false)}
                                    style={{
                                        display: 'flex',
                                        alignItems: 'center',
                                        justifyContent: 'center',
                                        gap: '6px',
                                        padding: '10px 16px', // Matches vertical height of other buttons
                                        height: '36px',
                                        backgroundColor: 'transparent',
                                        border: 'none',
                                        color: 'var(--text-secondary)', // Subtle color so the map buttons pop
                                        cursor: 'pointer',
                                        fontWeight: '600',
                                        fontSize: '13px',
                                        transition: 'color 0.2s ease'
                                    }}
                                    onMouseOver={(e) => e.currentTarget.style.color = 'var(--text-primary)'}
                                    onMouseOut={(e) => e.currentTarget.style.color = 'var(--text-secondary)'}
                                >
                                    Cancel
                                </button>
                            ) : (
                                // The standard Rating Badge
                                <div className="rating-badge">
                                    <Star size={14} className="text-blue" />
                                    <span>{displayClub.rating}</span>
                                </div>
                            )}

                            {/* --- RIGHT SIDE: Conditional between Map Choices or Navigate --- */}
                            {showMapChoice ? (
                                // The Choice Buttons
                                <div style={{ display: 'flex', gap: '8px' }}>

                                    {/* Apple Maps Button */}
                                    <button
                                        style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            gap: '6px',
                                            padding: '10px 12px',
                                            height: '36px',
                                            fontSize: '13px',
                                            fontWeight: '600',
                                            color: '#ffffff',
                                            backgroundColor: '#2A2D34', // Dark gray
                                            border: '1px solid #3F4451',
                                            borderRadius: '10px',
                                            cursor: 'pointer',
                                            transition: 'filter 0.2s ease'
                                        }}
                                        onMouseOver={(e) => e.currentTarget.style.filter = 'brightness(1.1)'}
                                        onMouseOut={(e) => e.currentTarget.style.filter = 'brightness(1)'}
                                        onClick={() => {
                                            localStorage.setItem('preferred_map_app', 'apple');
                                            window.open(`https://maps.apple.com/?q=${displayClub.lat},${displayClub.lng}`, '_blank');
                                            setShowMapChoice(false);
                                        }}
                                    >
                                        {/* SVG from previous step */}
                                        <svg width="14" height="16" viewBox="0 0 170 170" fill="currentColor">
                                            <path d="M125.74 81.3c-.15-20 16.32-29.65 17.06-30.1-9.35-13.7-23.93-15.6-29.23-15.82-12.48-1.27-24.36 7.4-30.65 7.4-6.3 0-16.03-7.14-26.25-6.95-13.43.2-25.8 7.8-32.65 19.74-13.84 24.1-3.54 59.78 9.94 79.35 6.6 9.56 14.36 20.3 24.6 19.9 9.85-.4 13.6-6.4 25.1-6.4 11.48 0 14.85 6.4 25.1 6.2 10.64-.2 17.34-9.8 23.83-19.34 7.55-11.13 10.66-21.84 10.82-22.42-.25-.1-21-8.1-21.16-30.94zM111.43 27.6c5.44-6.62 9.1-15.8 8.1-25-7.9 3.2-17.5 8.15-23.14 14.85-5.04 5.95-9.43 15.35-8.2 24.4 8.86.68 17.83-7.6 23.24-14.25z" />
                                        </svg>
                                        Apple
                                    </button>

                                    {/* Google Maps Button */}
                                    <button
                                        style={{
                                            display: 'flex',
                                            alignItems: 'center',
                                            justifyContent: 'center',
                                            gap: '6px',
                                            padding: '10px 12px',
                                            height: '36px',
                                            fontSize: '13px',
                                            fontWeight: '600',
                                            color: '#ffffff',
                                            backgroundColor: 'var(--accent-orange)',
                                            border: 'none',
                                            borderRadius: '10px',
                                            cursor: 'pointer',
                                            transition: 'filter 0.2s ease'
                                        }}
                                        onMouseOver={(e) => e.currentTarget.style.filter = 'brightness(0.9)'}
                                        onMouseOut={(e) => e.currentTarget.style.filter = 'brightness(1)'}
                                        onClick={() => {
                                            localStorage.setItem('preferred_map_app', 'google');
                                            window.open(`https://maps.google.com/?q=${displayClub.lat},${displayClub.lng}`, '_blank');
                                            setShowMapChoice(false);
                                        }}
                                    >
                                        {/* SVG G-Logo from previous step */}
                                        <svg width="15" height="15" viewBox="0 0 24 24" fill="currentColor">
                                            <path d="M12.48 10.92v3.28h7.84c-.24 1.84-.853 3.187-1.787 4.133-1.147 1.147-2.933 2.4-6.053 2.4-4.827 0-8.6-3.893-8.6-8.72s3.773-8.72 8.6-8.72c2.6 0 4.507 1.027 5.907 2.347l2.307-2.307C18.747 1.44 15.933 0 12.48 0 5.867 0 .307 5.387.307 12s5.56 12 12.173 12c3.573 0 6.267-1.173 8.373-3.36 2.16-2.16 2.84-5.213 2.84-7.667 0-.76-.053-1.467-.173-2.053H12.48z" />
                                        </svg>
                                        Google
                                    </button>
                                </div>
                            ) : (
                                <button
                                    className="navigate-btn"
                                    style={{ display: 'flex', alignItems: 'center', gap: '8px', justifyContent: 'center' }}
                                    onClick={() => {
                                        const { lat, lng } = displayClub;

                                        // 1. Check if they already made a choice in the past
                                        const savedPreference = localStorage.getItem('preferred_map_app');

                                        if (savedPreference === 'apple') {
                                            window.open(`https://maps.apple.com/?q=${lat},${lng}`, '_blank');
                                            return;
                                        }
                                        if (savedPreference === 'google') {
                                            window.open(`https://maps.google.com/?q=${lat},${lng}`, '_blank');
                                            return;
                                        }

                                        // 2. If no saved preference, check their OS
                                        const isIOS = /iPad|iPhone|iPod/.test(navigator.userAgent) ||
                                            (navigator.platform === 'MacIntel' && navigator.maxTouchPoints > 1);

                                        if (isIOS) {
                                            // Show the menu so they can make their permanent choice
                                            setShowMapChoice(true);
                                        } else {
                                            // Android/Desktop defaults to Google Maps
                                            window.open(`https://maps.google.com/?q=${lat},${lng}`, '_blank');
                                        }
                                    }}
                                >
                                    <Navigation size={18} />
                                    <span>Navigate</span>
                                </button>
                            )}
                        </div>
                    </div>
                </div>
            )}

            {/* CUSTOM BOTTOM SHEET */}
            <div
                className={`bottom-sheet state-${sheetState} ${isDragging ? 'dragging' : ''}`}
                style={{ transform: isDragging ? `translateY(calc(var(--sheet-base-y) + ${dragOffset}px))` : '' }}
                onPointerDown={handlePointerDown}
                onPointerMove={handlePointerMove}
                onPointerUp={handlePointerUp}
                onPointerCancel={handlePointerUp}
            >
                <div className="drag-handle-area">
                    <div className="drag-handle-bar" />
                </div>

                <div className="sheet-content" ref={contentRef}>
                    <h2 className="sheet-title">{t('nearby_clubs', 'Nearby Clubs')}</h2>
                    <div className="club-list">
                        {/* 👇 5. Render logic updated to check zoom level and use visibleClubs */}
                        {isLoading ? (
                            <p style={{ color: 'var(--text-secondary)', textAlign: 'center', padding: '20px' }}>
                                Loading nearby tables...
                            </p>
                        ) : currentZoom < MIN_ZOOM_TO_SHOW_CLUBS ? (
                            <p style={{ color: 'var(--text-secondary)', textAlign: 'center', padding: '20px' }}>
                                🗺️ Zoom in closer to see tables in this area.
                            </p>
                        ) : visibleClubs.length === 0 ? (
                            <p style={{ color: 'var(--text-secondary)', textAlign: 'center', padding: '20px' }}>
                                No tables found in this visible area.
                            </p>
                        ) : (
                            visibleClubs.map(club => (
                                <div key={club.id} className="club-list-item" onClick={() => handleClubClick(club)}>
                                    <div className="club-list-img-placeholder" />
                                    <div className="club-list-info">
                                        <h4>{club.name}</h4>
                                        <p>{club.distance} • {club.tables} Tables</p>
                                    </div>
                                </div>
                            ))
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}