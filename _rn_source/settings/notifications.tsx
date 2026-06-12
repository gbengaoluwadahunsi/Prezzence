import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import React, { useState } from 'react';
import {
    ScrollView,
    StyleSheet,
    Text,
    View,
    Pressable,
    Switch
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { colors, spacing, typography, radii } from '../../src/theme';
import { useSettingsStore } from '../../src/store/settingsStore';
import { cancelPracticeReminders, ensureNotificationPermission, schedulePracticeReminders } from '../../src/services/notifications';
import { getPracticeGoal } from '../../src/api/users';

function NotificationItem({ title, subtitle, enabled, onToggle }: { title: string, subtitle: string, enabled: boolean, onToggle: (v: boolean) => void }) {
    return (
        <View style={styles.item}>
            <View style={styles.info}>
                <Text style={styles.itemTitle}>{title}</Text>
                <Text style={styles.itemSubtitle}>{subtitle}</Text>
            </View>
            <Switch
                value={enabled}
                onValueChange={onToggle}
                trackColor={{ false: 'rgba(255,255,255,0.1)', true: colors.accent }}
                thumbColor="#FFFFFF"
            />
        </View>
    );
}

export default function NotificationsSettingsScreen() {
    const {
        pushNotificationsEnabled,
        practiceRemindersEnabled,
        achievementAlertsEnabled,
        setNotificationPreference,
    } = useSettingsStore();
    const [pushEnabled, setPushEnabled] = useState(pushNotificationsEnabled);
    const [emailEnabled, setEmailEnabled] = useState(false);
    const [sessionAlerts, setSessionAlerts] = useState(practiceRemindersEnabled);
    const [achievements, setAchievements] = useState(achievementAlertsEnabled);

    const togglePush = async (enabled: boolean) => {
        if (enabled) {
            const granted = await ensureNotificationPermission();
            if (!granted) return;
        } else {
            await cancelPracticeReminders();
        }
        setPushEnabled(enabled);
        setNotificationPreference('pushNotificationsEnabled', enabled);
    };

    const toggleSessionAlerts = async (enabled: boolean) => {
        setSessionAlerts(enabled);
        setNotificationPreference('practiceRemindersEnabled', enabled);
        if (!enabled) {
            await cancelPracticeReminders();
            return;
        }
        const goal = await getPracticeGoal().catch(() => ({ daily_minutes: 10, interview_date: null }));
        await schedulePracticeReminders({
            dailyMinutes: goal.daily_minutes,
            interviewDate: goal.interview_date,
            enabled,
        });
        setPushEnabled(true);
        setNotificationPreference('pushNotificationsEnabled', true);
    };

    const toggleAchievements = (enabled: boolean) => {
        setAchievements(enabled);
        setNotificationPreference('achievementAlertsEnabled', enabled);
    };

    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <Pressable onPress={() => router.back()} style={styles.backBtn}>
                    <Feather name="chevron-left" size={24} color="#FFFFFF" />
                </Pressable>
                <Text style={styles.headerTitle}>Notifications</Text>
                <View style={{ width: 44 }} />
            </View>

            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
                <View style={styles.titleSection}>
                    <Text style={styles.headline}>Notifications</Text>
                    <Text style={styles.subheadline}>Choose when Prezzence reminds you to practice or review progress.</Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTag}>CHANNELS</Text>
                    <View style={styles.card}>
                        <NotificationItem
                            title="Push notifications"
                            subtitle="Practice reminders on this device"
                            enabled={pushEnabled}
                            onToggle={togglePush}
                        />
                        <View style={styles.divider} />
                        <NotificationItem
                            title="Email summaries"
                            subtitle="Weekly progress and coaching summaries"
                            enabled={emailEnabled}
                            onToggle={setEmailEnabled}
                        />
                    </View>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTag}>ACTIVITY ALERTS</Text>
                    <View style={styles.card}>
                        <NotificationItem
                            title="Practice reminders"
                            subtitle="Get reminded before your interview date"
                            enabled={sessionAlerts}
                            onToggle={toggleSessionAlerts}
                        />
                        <View style={styles.divider} />
                        <NotificationItem
                            title="Milestone alerts"
                            subtitle="Get alerts when your score or streak improves"
                            enabled={achievements}
                            onToggle={toggleAchievements}
                        />
                        <View style={styles.divider} />
                        <NotificationItem
                            title="Product updates"
                            subtitle="New features and useful interview tips"
                            enabled={true}
                            onToggle={() => { }}
                        />
                    </View>
                </View>
            </ScrollView>

            <View style={styles.footer}>
                <Pressable
                    onPress={() => router.back()}
                    style={({ pressed }) => [
                        styles.saveButton,
                        pressed && { opacity: 0.9 }
                    ]}
                >
                    <Text style={styles.saveText}>Done</Text>
                </Pressable>
            </View>
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
        paddingBottom: 120,
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
        borderRadius: 32,
        padding: 8,
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.05)',
    },
    item: {
        flexDirection: 'row',
        alignItems: 'center',
        padding: 20,
    },
    info: {
        flex: 1,
        paddingRight: 20,
    },
    itemTitle: {
        color: '#FFFFFF',
        fontSize: 17,
        fontWeight: '800',
    },
    itemSubtitle: {
        color: colors.textMuted,
        fontSize: 13,
        marginTop: 2,
        opacity: 0.7,
    },
    divider: {
        height: 1,
        backgroundColor: 'rgba(255, 255, 255, 0.03)',
        marginHorizontal: 20,
    },
    footer: {
        position: 'absolute',
        bottom: 0,
        left: 0,
        right: 0,
        padding: 24,
        paddingBottom: 40,
        backgroundColor: 'rgba(0,0,0,0.8)',
    },
    saveButton: {
        backgroundColor: colors.accent,
        height: 64,
        borderRadius: 32,
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: colors.accent,
        shadowOpacity: 0.4,
        shadowRadius: 15,
        elevation: 10,
    },
    saveText: {
        color: '#FFFFFF',
        fontSize: 18,
        fontWeight: '800',
    },
});
