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

function AccessibilityItem({ icon, title, subtitle, enabled }: { icon: keyof typeof Feather.glyphMap, title: string, subtitle: string, enabled: boolean }) {
    return (
        <View style={styles.card}>
            <View style={styles.iconBox}>
                <Feather name={icon as any} size={20} color={colors.accent} />
            </View>
            <View style={styles.info}>
                <Text style={styles.cardTitle}>{title}</Text>
                <Text style={styles.cardSubtitle}>{subtitle}</Text>
            </View>
            <View style={[styles.toggleTrack, enabled && { backgroundColor: colors.accent }]}>
                <View style={[styles.toggleThumb, enabled && { alignSelf: 'flex-end' }]} />
            </View>
        </View>
    );
}

export default function AccessibilityScreen() {
    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <Pressable onPress={() => router.back()} style={styles.backBtn}>
                    <Feather name="chevron-left" size={24} color="#FFFFFF" />
                </Pressable>
                <Text style={styles.headerTitle}>Accessibility</Text>
                <View style={{ width: 44 }} />
            </View>

            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
                <View style={styles.titleSection}>
                    <Text style={styles.headline}>Accessibility</Text>
                    <Text style={styles.subheadline}>Adjust text, motion, and audio settings to make the app easier to use.</Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTag}>VISION & DISPLAY</Text>
                    <AccessibilityItem
                        icon="type"
                        title="Large text"
                        subtitle="200% font scale across all screens"
                        enabled={true}
                    />
                    <AccessibilityItem
                        icon="eye"
                        title="High contrast"
                        subtitle="Enhanced edge definition for UI elements"
                        enabled={false}
                    />
                    <AccessibilityItem
                        icon="layers"
                        title="Reduced Motion"
                        subtitle="Minimize animations and motion effects"
                        enabled={false}
                    />
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTag}>AUDIO & FEEDBACK</Text>
                    <AccessibilityItem
                        icon="mic"
                        title="Screen reader support"
                        subtitle="Clear voice and label support"
                        enabled={false}
                    />
                    <AccessibilityItem
                        icon="radio"
                        title="Mono Audio"
                        subtitle="Combine channels for single-ear focus"
                        enabled={false}
                    />
                </View>

                <View style={styles.previewCard}>
                    <View style={styles.previewHeader}>
                        <Feather name="check-circle" size={18} color={colors.success} />
                        <Text style={styles.previewTag}>PREVIEW</Text>
                    </View>
                    <Text style={styles.previewTitle}>Easy to read</Text>
                    <Text style={styles.previewText}>
                        "Tell me about a time you solved a difficult problem." Text should stay readable without clipping.
                    </Text>
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
        backgroundColor: 'rgba(255,255,255,0.1)',
        padding: 2,
        justifyContent: 'center',
    },
    toggleThumb: {
        width: 20,
        height: 20,
        borderRadius: 10,
        backgroundColor: '#FFFFFF',
    },
    previewCard: {
        backgroundColor: 'rgba(255, 255, 255, 0.02)',
        borderRadius: 32,
        padding: 32,
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.05)',
        marginBottom: 32,
    },
    previewHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 8,
        marginBottom: 16,
    },
    previewTag: {
        color: colors.success,
        fontSize: 10,
        fontWeight: '900',
        letterSpacing: 1.5,
    },
    previewTitle: {
        color: '#FFFFFF',
        fontSize: 28,
        fontWeight: '900',
        marginBottom: 12,
    },
    previewText: {
        color: '#FFFFFF',
        fontSize: 20,
        lineHeight: 30,
        opacity: 0.7,
    },
});
