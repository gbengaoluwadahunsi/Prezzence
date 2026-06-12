import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import React from 'react';
import {
    ScrollView,
    StyleSheet,
    Text,
    View,
    Pressable
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { colors, spacing, typography, radii } from '../../src/theme';
import { useAuthStore } from '../../src/store/authStore';
import { useSessionStore } from '../../src/store/sessionStore';
import { useSettingsStore } from '../../src/store/settingsStore';
import { BETA_UNLOCK_ALL_FEATURES } from '../../src/config/featureFlags';
import { requestPresenceCameraPermission } from '../../src/native/PresenceCameraView';

function SettingItem({ icon, title, subtitle, onPress }: { icon: keyof typeof Feather.glyphMap, title: string, subtitle: string, onPress?: () => void }) {
    return (
        <Pressable
            onPress={onPress}
            style={({ pressed }) => [
                styles.card,
                pressed && { opacity: 0.8 }
            ]}
        >
            <View style={styles.iconBox}>
                <Feather name={icon as any} size={20} color={colors.accent} />
            </View>
            <View style={styles.info}>
                <Text style={styles.cardTitle}>{title}</Text>
                <Text style={styles.cardSubtitle}>{subtitle}</Text>
            </View>
            <Feather name="chevron-right" size={20} color={colors.textMuted} />
        </Pressable>
    );
}

function SettingToggle({
    icon,
    title,
    subtitle,
    enabled,
    onToggle,
}: {
    icon: keyof typeof Feather.glyphMap;
    title: string;
    subtitle: string;
    enabled: boolean;
    onToggle: () => void;
}) {
    return (
        <Pressable
            onPress={onToggle}
            style={({ pressed }) => [
                styles.card,
                pressed && { opacity: 0.8 }
            ]}
            accessibilityRole="switch"
            accessibilityState={{ checked: enabled }}
        >
            <View style={styles.iconBox}>
                <Feather name={icon as any} size={20} color={colors.accent} />
            </View>
            <View style={styles.info}>
                <Text style={styles.cardTitle}>{title}</Text>
                <Text style={styles.cardSubtitle}>{subtitle}</Text>
            </View>
            <View style={[styles.toggleTrack, !enabled && styles.toggleTrackOff]}>
                <View style={[styles.toggleThumb, !enabled && styles.toggleThumbOff]} />
            </View>
        </Pressable>
    );
}

export default function AppSettingsScreen() {
    const { signOut } = useAuthStore();
    const { language, isPremium } = useSessionStore();
    const { cameraPresenceCoachEnabled, setCameraPresenceCoachEnabled } = useSettingsStore();
    const hasPremiumAccess = BETA_UNLOCK_ALL_FEATURES || isPremium;
    const handleSignOut = () => {
        signOut();
        router.replace('/auth/sign-in' as any);
    };
    const togglePresenceCoach = async () => {
        if (cameraPresenceCoachEnabled) {
            setCameraPresenceCoachEnabled(false);
            return;
        }

        const granted = await requestPresenceCameraPermission();
        if (granted) {
            setCameraPresenceCoachEnabled(true);
        }
    };

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <Pressable onPress={() => router.back()} style={styles.backBtn}>
                    <Feather name="chevron-left" size={24} color="#FFFFFF" />
                </Pressable>
                <Text style={styles.headerTitle}>Settings</Text>
                <Pressable
                    style={styles.circleBtn}
                    onPress={() => router.push('/profile/notifications' as any)}
                    accessibilityRole="button"
                    accessibilityLabel="Open notifications"
                >
                    <Feather name="bell" size={20} color="#FFFFFF" />
                </Pressable>
            </View>

            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
                <View style={styles.titleSection}>
                    <Text style={styles.headline}>Settings</Text>
                    <Text style={styles.subheadline}>Manage account access, language, subscription, privacy, and interview preferences.</Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTag}>INTERVIEW SETUP</Text>
                    <SettingToggle
                        icon="video"
                        title="Camera presence coach"
                        subtitle={cameraPresenceCoachEnabled
                            ? 'Enabled. Uses your camera during answers only; feedback stays on this device'
                            : 'Off. Turn on before starting an interview for face, eyes, posture, and energy feedback'}
                        enabled={cameraPresenceCoachEnabled}
                        onToggle={togglePresenceCoach}
                    />
                    <SettingItem
                        icon="volume-2"
                        title="Interviewer setup"
                        subtitle="Choose one interviewer or a panel, plus the interviewer style"
                        onPress={() => router.push('/onboarding/role' as any)}
                    />
                    <SettingItem
                        icon="globe"
                        title="App language"
                        subtitle={`Current: ${language.toUpperCase()}`}
                        onPress={() => router.push({ pathname: '/auth/language', params: { returnTo: 'back' } } as any)}
                    />
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTag}>ACCOUNT</Text>
                    <SettingItem
                        icon="user"
                        title="Account details"
                        subtitle="Name, email, password reset, and your practice profile"
                        onPress={() => router.push('/settings/account' as any)}
                    />
                    <SettingItem
                        icon="credit-card"
                        title="Subscription"
                        subtitle={hasPremiumAccess ? 'Beta access active for all testers' : 'Free plan, upgrade and restore purchases'}
                        onPress={() => router.push('/profile/subscription' as any)}
                    />
                    <SettingItem
                        icon="shield"
                        title="Privacy and deletion"
                        subtitle="Terms, privacy policy, consent, and account deletion"
                        onPress={() => router.push('/profile/privacy' as any)}
                    />
                    <SettingItem
                        icon="bell"
                        title="Notifications"
                        subtitle="Practice reminders and score alerts"
                        onPress={() => router.push('/settings/notifications' as any)}
                    />
                </View>

                <View style={styles.section}>
                    <Pressable style={styles.logoutBtn} onPress={handleSignOut}>
                        <Feather name="log-out" size={18} color="#FF4B4B" />
                        <Text style={styles.logoutText}>Sign out</Text>
                    </Pressable>
                </View>
            </ScrollView>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: colors.background,
    },
    scrollContent: {
        paddingHorizontal: 24,
        paddingBottom: 60,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 16,
        height: 80,
    },
    backBtn: {
        width: 44,
        height: 44,
        backgroundColor: colors.card,
        borderRadius: 22,
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.05)',
    },
    circleBtn: {
        width: 44,
        height: 44,
        alignItems: 'center',
        justifyContent: 'center',
    },
    headerTitle: {
        color: '#FFFFFF',
        fontSize: 16,
        fontWeight: '800',
    },
    titleSection: {
        marginTop: 20,
        marginBottom: 32,
    },
    headline: {
        color: '#FFFFFF',
        fontSize: 40,
        fontWeight: '900',
        letterSpacing: -1.5,
    },
    subheadline: {
        color: colors.textMuted,
        fontSize: 16,
        marginTop: 8,
        lineHeight: 24,
        opacity: 0.8,
    },
    section: {
        marginBottom: 32,
    },
    sectionTag: {
        color: colors.accent,
        fontSize: 10,
        fontWeight: '900',
        letterSpacing: 2,
        marginBottom: 16,
    },
    card: {
        backgroundColor: colors.card,
        borderRadius: 28,
        padding: 20,
        flexDirection: 'row',
        alignItems: 'center',
        marginBottom: 12,
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.05)',
    },
    iconBox: {
        width: 48,
        height: 48,
        borderRadius: 16,
        backgroundColor: 'rgba(108, 99, 255, 0.1)',
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 16,
    },
    info: {
        flex: 1,
    },
    cardTitle: {
        color: '#FFFFFF',
        fontSize: 17,
        fontWeight: '900',
        letterSpacing: -0.2,
    },
    cardSubtitle: {
        color: colors.textMuted,
        fontSize: 13,
        marginTop: 2,
        opacity: 0.7,
    },
    toggleTrack: {
        width: 44,
        height: 24,
        borderRadius: 12,
        backgroundColor: colors.accent,
        padding: 2,
        justifyContent: 'center',
        alignItems: 'flex-end',
    },
    toggleTrackOff: {
        backgroundColor: 'rgba(255,255,255,0.12)',
        alignItems: 'flex-start',
    },
    toggleThumb: {
        width: 20,
        height: 20,
        borderRadius: 10,
        backgroundColor: '#FFFFFF',
    },
    toggleThumbOff: {
        backgroundColor: 'rgba(255,255,255,0.72)',
    },
    logoutBtn: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 10,
        padding: 20,
        borderRadius: 24,
        backgroundColor: 'rgba(255, 75, 75, 0.05)',
        borderWidth: 1,
        borderColor: 'rgba(255, 75, 75, 0.1)',
    },
    logoutText: {
        color: '#FF4B4B',
        fontSize: 16,
        fontWeight: '800',
    },
});
