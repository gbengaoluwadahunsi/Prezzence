import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import React from 'react';
import {
    ScrollView,
    StyleSheet,
    Text,
    View,
    Pressable,
    Platform,
    ActivityIndicator,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { LinearGradient } from 'expo-linear-gradient';

import { useSessionStore } from '../../src/store/sessionStore';
import { deleteSession, getSessions } from '../../src/api/sessions';
import { ConfirmSheet } from '../../src/components/ConfirmSheet';

const Colors = {
    bgPrimary: '#0A0A0F',
    accent: '#6C63FF',
    textSecondary: '#8A8A9A',
    card: '#1C1C2E',
    border: '#2A2A3E',
};

export default function PracticeHomeScreen() {
    const { activeSessionId, setInterviewType, setInterviewerMode, resetSession } = useSessionStore();
    const [sessions, setSessions] = React.useState<Array<{
        id: string;
        title: string;
        type: string;
        date: string;
        score: number;
        status: string;
    }>>([]);
    const [loadingSessions, setLoadingSessions] = React.useState(true);
    const [deletingSessionId, setDeletingSessionId] = React.useState<string | null>(null);
    const [pendingDeleteSession, setPendingDeleteSession] = React.useState<{ id: string; title: string } | null>(null);

    const loadSessions = React.useCallback(async (mounted = true) => {
        try {
            const data = await getSessions();
            if (mounted) {
                setSessions(data.sessions || []);
            }
        } catch (error) {
            console.error('[Sessions] Failed to load sessions:', error);
        } finally {
            if (mounted) {
                setLoadingSessions(false);
            }
        }
    }, []);

    React.useEffect(() => {
        let mounted = true;

        loadSessions(mounted);
        return () => {
            mounted = false;
        };
    }, [loadSessions]);

    const practiceModes = [
        {
            id: 'full',
            icon: 'users',
            title: 'Full interview',
            description: 'Practice a complete interview from start to finish.',
            color: '#7C3AED',
            active: true
        },
        {
            id: 'quick',
            icon: 'clock',
            title: 'Quick practice',
            description: 'Answer a few short questions when you have limited time.',
            color: '#0EA5E9',
            active: true
        },
        {
            id: 'behavioral',
            icon: 'message-square',
            title: 'Story questions',
            description: 'Practice examples about teamwork, leadership, and problem solving.',
            color: '#10B981',
            active: true
        },
        {
            id: 'technical',
            icon: 'briefcase',
            title: 'Role skills',
            description: 'Practice questions about the real work in your target role.',
            color: '#F59E0B',
            active: true
        },
        {
            id: 'promotion',
            icon: 'trending-up',
            title: 'Leadership questions',
            description: 'Prepare for manager, senior, promotion, or impact questions.',
            color: '#EF4444',
            active: true
        },
    ];

    const handleSelectMode = (mode: any) => {
        if (!mode.active) return;

        resetSession();
        setInterviewerMode(mode.id === 'full' ? 'panel' : 'single');
        setInterviewType(mode.id === 'full' ? 'mixed' : mode.id);

        // Navigate to role selection
        router.push('/onboarding/role' as any);
    };

    const handleDeleteSession = React.useCallback((session: { id: string; title: string }) => {
        setPendingDeleteSession(session);
    }, []);

    const confirmDeleteSession = React.useCallback(async () => {
        if (!pendingDeleteSession) return;
        try {
            setDeletingSessionId(pendingDeleteSession.id);
            await deleteSession(pendingDeleteSession.id);
            setSessions((current) => current.filter((item) => item.id !== pendingDeleteSession.id));
            if (pendingDeleteSession.id === activeSessionId) {
                resetSession();
            }
            setPendingDeleteSession(null);
        } catch (error: any) {
            console.error('[Sessions] Failed to delete session:', error);
        } finally {
            setDeletingSessionId(null);
        }
    }, [pendingDeleteSession]);

    return (
        <SafeAreaView style={styles.container}>
            <ConfirmSheet
                visible={Boolean(pendingDeleteSession)}
                title="Delete session?"
                message={`Remove "${pendingDeleteSession?.title || 'this session'}" from your interview record? This cannot be undone.`}
                confirmLabel="Delete"
                danger
                loading={Boolean(pendingDeleteSession && deletingSessionId === pendingDeleteSession.id)}
                onCancel={() => {
                    if (!deletingSessionId) setPendingDeleteSession(null);
                }}
                onConfirm={confirmDeleteSession}
            />
            <LinearGradient
                colors={['rgba(124, 58, 237, 0.05)', 'transparent']}
                style={StyleSheet.absoluteFill}
            />
            <ScrollView
                style={{ flex: 1 }}
                contentContainerStyle={styles.scrollContent}
                showsVerticalScrollIndicator={false}
            >
                {/* Header */}
                <View style={styles.header}>
                    <Text style={styles.headerTitle}>Practice</Text>
                </View>

                <View style={styles.titleSection}>
                    <Text style={styles.headline}>What do you want to practice?</Text>
                    <Text style={styles.subheadline}>Choose the interview practice that matches your next step.</Text>
                </View>

                <View style={styles.sessionsSection}>
                    <View style={styles.sectionHeader}>
                        <Text style={styles.sectionTitle}>Recent sessions</Text>
                        <Text style={styles.sectionCount}>{sessions.length}</Text>
                    </View>

                    {loadingSessions ? (
                        <View style={styles.loadingCard}>
                            <ActivityIndicator color={Colors.accent} />
                            <Text style={styles.loadingText}>Loading sessions...</Text>
                        </View>
                    ) : sessions.length === 0 ? (
                        <View style={styles.emptyCard}>
                            <Text style={styles.emptyTitle}>No sessions yet</Text>
                            <Text style={styles.emptyText}>Your completed interviews will appear here after your first practice session.</Text>
                        </View>
                    ) : (
                        sessions.map((session) => (
                            <Pressable
                                key={session.id}
                                onPress={() => router.push(`/sessions/${session.id}` as any)}
                                style={({ pressed }) => [styles.sessionCard, pressed && { opacity: 0.85 }]}
                            >
                                <View style={styles.sessionScore}>
                                    <Text style={styles.sessionScoreValue}>{session.score || 0}</Text>
                                    <Text style={styles.sessionScoreUnit}>%</Text>
                                </View>
                                <View style={styles.sessionInfo}>
                                    <Text style={styles.sessionTitle} numberOfLines={1}>{session.title}</Text>
                                    <Text style={styles.sessionMeta}>{session.type} • {session.date}</Text>
                                </View>
                                <View style={styles.sessionActions}>
                                    <View style={[
                                        styles.statusPill,
                                        session.status === 'completed' ? styles.statusComplete : styles.statusOpen
                                    ]}>
                                        <Text style={styles.statusText}>{session.status || 'open'}</Text>
                                    </View>
                                    <Pressable
                                        onPress={(event) => {
                                            event.stopPropagation();
                                            handleDeleteSession(session);
                                        }}
                                        disabled={deletingSessionId === session.id}
                                        hitSlop={10}
                                        style={({ pressed }) => [
                                            styles.deleteBtn,
                                            deletingSessionId === session.id && styles.deleteBtnDisabled,
                                            pressed && { opacity: 0.75 }
                                        ]}
                                    >
                                        {deletingSessionId === session.id ? (
                                            <ActivityIndicator size="small" color="#FF8A9A" />
                                        ) : (
                                            <Feather name="trash-2" size={16} color="#FF8A9A" />
                                        )}
                                    </Pressable>
                                </View>
                            </Pressable>
                        ))
                    )}
                </View>

                <View style={styles.sectionHeaderPadded}>
                    <Text style={styles.sectionTitle}>Practice options</Text>
                </View>

                <View style={styles.stageList}>
                    {practiceModes.map((mode) => (
                        <Pressable
                            key={mode.id}
                            onPress={() => handleSelectMode(mode)}
                            style={[
                                styles.stageCard,
                                mode.active && styles.modeCardActive,
                                !mode.active && { opacity: 0.5 }
                            ]}
                        >
                            <View style={[styles.iconBox, { backgroundColor: mode.color + '10', borderColor: mode.color + '20' }]}>
                                <Feather name={mode.icon as any} size={23} color={mode.color} />
                                {mode.active && (
                                    <View style={[styles.activeGlow, { backgroundColor: mode.color }]} />
                                )}
                            </View>

                            <View style={styles.stageCopy}>
                                <Text style={styles.modeTitle}>{mode.title}</Text>
                                <Text style={styles.modeDesc}>{mode.description}</Text>
                            </View>
                            <Feather name="chevron-right" size={20} color={Colors.textSecondary} />
                        </Pressable>
                    ))}
                </View>
            </ScrollView>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: Colors.bgPrimary,
    },
    scrollContent: {
        paddingBottom: 150,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 20,
        height: 60,
        marginTop: 10,
    },
    circleBtn: {
        width: 44,
        height: 44,
        borderRadius: 22,
        backgroundColor: 'rgba(255, 255, 255, 0.05)',
        alignItems: 'center',
        justifyContent: 'center',
    },
    headerTitle: {
        color: '#FFFFFF',
        fontSize: 20,
        fontWeight: '900',
        letterSpacing: -0.5,
    },
    titleSection: {
        paddingHorizontal: 24,
        marginTop: 20,
        marginBottom: 32,
    },
    headline: {
        color: '#FFFFFF',
        fontSize: 42,
        fontWeight: '900',
        lineHeight: 48,
        letterSpacing: -1,
    },
    subheadline: {
        color: Colors.textSecondary,
        fontSize: 16,
        marginTop: 12,
        lineHeight: 24,
        fontWeight: '500',
    },
    sessionsSection: {
        paddingHorizontal: 20,
        marginBottom: 28,
    },
    sectionHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        marginBottom: 14,
    },
    sectionHeaderPadded: {
        paddingHorizontal: 24,
        marginBottom: 14,
    },
    sectionTitle: {
        color: '#FFFFFF',
        fontSize: 18,
        fontWeight: '900',
        letterSpacing: -0.2,
    },
    sectionCount: {
        color: Colors.accent,
        fontSize: 14,
        fontWeight: '900',
    },
    loadingCard: {
        minHeight: 96,
        borderRadius: 24,
        backgroundColor: 'rgba(255, 255, 255, 0.03)',
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.06)',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 10,
    },
    loadingText: {
        color: Colors.textSecondary,
        fontSize: 13,
        fontWeight: '600',
    },
    emptyCard: {
        borderRadius: 24,
        backgroundColor: 'rgba(255, 255, 255, 0.03)',
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.06)',
        padding: 20,
    },
    emptyTitle: {
        color: '#FFFFFF',
        fontSize: 16,
        fontWeight: '900',
        marginBottom: 6,
    },
    emptyText: {
        color: Colors.textSecondary,
        fontSize: 14,
        lineHeight: 20,
    },
    sessionCard: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: 'rgba(255, 255, 255, 0.03)',
        borderRadius: 24,
        padding: 16,
        marginBottom: 12,
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.06)',
    },
    sessionScore: {
        width: 58,
        height: 58,
        borderRadius: 18,
        backgroundColor: 'rgba(108, 99, 255, 0.14)',
        alignItems: 'center',
        justifyContent: 'center',
        flexDirection: 'row',
    },
    sessionScoreValue: {
        color: '#FFFFFF',
        fontSize: 20,
        fontWeight: '900',
    },
    sessionScoreUnit: {
        color: Colors.textSecondary,
        fontSize: 11,
        fontWeight: '800',
        marginLeft: 1,
        marginTop: 7,
    },
    sessionInfo: {
        flex: 1,
        marginLeft: 14,
        marginRight: 10,
    },
    sessionTitle: {
        color: '#FFFFFF',
        fontSize: 15,
        fontWeight: '900',
        marginBottom: 4,
    },
    sessionMeta: {
        color: Colors.textSecondary,
        fontSize: 12,
        fontWeight: '600',
    },
    statusPill: {
        borderRadius: 999,
        paddingHorizontal: 9,
        paddingVertical: 5,
    },
    statusComplete: {
        backgroundColor: 'rgba(16, 185, 129, 0.12)',
    },
    statusOpen: {
        backgroundColor: 'rgba(245, 158, 11, 0.12)',
    },
    statusText: {
        color: '#FFFFFF',
        fontSize: 10,
        fontWeight: '900',
        textTransform: 'uppercase',
    },
    sessionActions: {
        alignItems: 'flex-end',
        gap: 8,
    },
    deleteBtn: {
        width: 34,
        height: 34,
        borderRadius: 17,
        backgroundColor: 'rgba(255, 71, 87, 0.12)',
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: 1,
        borderColor: 'rgba(255, 71, 87, 0.22)',
    },
    deleteBtnDisabled: {
        opacity: 0.55,
    },
    stageList: {
        paddingHorizontal: 20,
        gap: 12,
    },
    stageCard: {
        minHeight: 92,
        backgroundColor: 'rgba(255, 255, 255, 0.03)',
        borderRadius: 24,
        padding: 16,
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.06)',
        flexDirection: 'row',
        alignItems: 'center',
        gap: 14,
        ...Platform.select({
            ios: {
                shadowColor: '#000',
                shadowOffset: { width: 0, height: 4 },
                shadowOpacity: 0.3,
                shadowRadius: 8,
            },
            android: {
                elevation: 4,
            },
        }),
    },
    modeCardActive: {
        borderColor: 'rgba(124, 58, 237, 0.4)',
        backgroundColor: 'rgba(124, 58, 237, 0.04)',
    },
    iconBox: {
        width: 52,
        height: 52,
        borderRadius: 18,
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: 1,
        position: 'relative',
    },
    stageCopy: {
        flex: 1,
        minWidth: 0,
    },
    activeGlow: {
        position: 'absolute',
        width: '100%',
        height: '100%',
        borderRadius: 20,
        opacity: 0.15,
        zIndex: -1,
    },
    modeTitle: {
        color: '#FFFFFF',
        fontSize: 17,
        fontWeight: '900',
        marginBottom: 4,
        letterSpacing: -0.3,
    },
    modeDesc: {
        color: Colors.textSecondary,
        fontSize: 13,
        lineHeight: 18,
        fontWeight: '600',
    },
    lockBadge: {
        position: 'absolute',
        top: -6,
        right: -6,
        width: 24,
        height: 24,
        borderRadius: 12,
        backgroundColor: Colors.accent,
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: 2,
        borderColor: Colors.bgPrimary,
    },
});
