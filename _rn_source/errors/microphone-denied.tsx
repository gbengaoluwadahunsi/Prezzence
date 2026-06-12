import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import React from 'react';
import {
    ScrollView,
    StyleSheet,
    Text,
    View,
    Pressable,
    Linking
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { colors, spacing, typography, radii } from '../../src/theme';

function HelpStep({ icon, title, subtitle, onPress }: { icon: keyof typeof Feather.glyphMap, title: string, subtitle: string, onPress?: () => void }) {
    return (
        <Pressable
            onPress={onPress}
            style={({ pressed }) => [
                styles.card,
                pressed && { opacity: 0.8 }
            ]}
        >
            <View style={styles.iconBox}>
                <Feather name={icon as any} size={22} color={colors.accent} />
            </View>
            <View style={styles.info}>
                <Text style={styles.cardTitle}>{title}</Text>
                <Text style={styles.cardSubtitle}>{subtitle}</Text>
            </View>
            <Feather name="external-link" size={18} color={colors.textMuted} />
        </Pressable>
    );
}

export default function MicrophoneDeniedScreen() {
    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <Pressable onPress={() => router.back()} style={styles.backBtn}>
                    <Feather name="x" size={24} color="#FFFFFF" />
                </Pressable>
                <Text style={styles.headerTitle}>Microphone access</Text>
                <View style={{ width: 44 }} />
            </View>

            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
                <View style={styles.titleSection}>
                    <Text style={styles.headline}>Microphone is blocked</Text>
                    <Text style={styles.subheadline}>Prezzence needs microphone access to record your answer and give feedback.</Text>
                </View>

                <View style={styles.lockoutField}>
                    <View style={styles.lockoutOrb}>
                        <Feather name="mic-off" size={48} color="#FF4B4B" />
                        <View style={styles.lockoutRing} />
                        <View style={[styles.lockoutRing, { width: 160, height: 160, borderRadius: 80, borderColor: 'rgba(255, 75, 75, 0.05)' }]} />
                    </View>
                    <View style={styles.statusBadge}>
                        <Text style={styles.statusText}>MIC BLOCKED</Text>
                    </View>
                </View>

                <View style={styles.list}>
                    <Text style={styles.sectionLabel}>HOW TO FIX IT</Text>
                    <HelpStep
                        icon="settings"
                        title="Phone settings"
                        subtitle="Allow microphone access in your device settings"
                        onPress={() => Linking.openSettings()}
                    />
                    <HelpStep
                        icon="shield"
                        title="Privacy"
                        subtitle="Recording starts only when you answer a question"
                        onPress={() => router.push('/settings/privacy')}
                    />
                    <HelpStep
                        icon="help-circle"
                        title="Microphone check"
                        subtitle="Make sure your microphone works in other apps"
                        onPress={() => router.replace('/onboarding/mic-permission' as any)}
                    />
                </View>
            </ScrollView>

            <View style={styles.footer}>
                <Pressable
                    onPress={() => Linking.openSettings()}
                    style={({ pressed }) => [
                        styles.primaryButton,
                        pressed && { opacity: 0.9 }
                    ]}
                >
                    <Text style={styles.buttonText}>Open Device Settings</Text>
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
        marginBottom: 40,
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
    lockoutField: {
        height: 260,
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: 40,
    },
    lockoutOrb: {
        width: 120,
        height: 120,
        borderRadius: 60,
        backgroundColor: 'rgba(255, 75, 75, 0.05)',
        alignItems: 'center',
        justifyContent: 'center',
    },
    lockoutRing: {
        position: 'absolute',
        width: 140,
        height: 140,
        borderRadius: 70,
        borderWidth: 1,
        borderColor: 'rgba(255, 75, 75, 0.2)',
        borderStyle: 'dashed',
    },
    statusBadge: {
        marginTop: 24,
        paddingHorizontal: 16,
        paddingVertical: 8,
        backgroundColor: 'rgba(255, 75, 75, 0.1)',
        borderRadius: 12,
        borderWidth: 1,
        borderColor: 'rgba(255, 75, 75, 0.2)',
    },
    statusText: {
        color: '#FF4B4B',
        fontSize: 12,
        fontWeight: '900',
        letterSpacing: 2,
    },
    list: {
        gap: 12,
    },
    sectionLabel: {
        color: colors.accent,
        fontSize: 10,
        fontWeight: '900',
        letterSpacing: 2,
        marginBottom: 8,
    },
    card: {
        backgroundColor: colors.card,
        borderRadius: 28,
        padding: 20,
        flexDirection: 'row',
        alignItems: 'center',
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
    },
    cardSubtitle: {
        color: colors.textMuted,
        fontSize: 13,
        marginTop: 2,
        opacity: 0.7,
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
    primaryButton: {
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
    buttonText: {
        color: '#FFFFFF',
        fontSize: 18,
        fontWeight: '800',
    },
});
