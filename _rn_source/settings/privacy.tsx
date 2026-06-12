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

export default function PrivacySettingsScreen() {
    return (
        <SafeAreaView style={styles.container}>
            <View style={styles.header}>
                <Pressable onPress={() => router.back()} style={styles.backBtn}>
                    <Feather name="chevron-left" size={24} color="#FFFFFF" />
                </Pressable>
                <Text style={styles.headerTitle}>Privacy</Text>
                <View style={{ width: 44 }} />
            </View>

            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
                <View style={styles.titleSection}>
                    <Text style={styles.headline}>Privacy</Text>
                    <Text style={styles.subheadline}>Control how your interview recordings, transcripts, scores, and account data are handled.</Text>
                </View>

                <View style={styles.securityCard}>
                    <View style={styles.securityHeader}>
                        <Feather name="shield" size={24} color={colors.success} />
                        <Text style={styles.securityTitle}>Protected data</Text>
                    </View>
                    <Text style={styles.securityText}>Your voice data, transcripts, and scores are stored privately and used to power your own coaching.</Text>
                </View>

                <View style={styles.section}>
                    <Text style={styles.sectionTag}>PRIVACY CONTROLS</Text>

                    <Pressable style={styles.privacyItem}>
                        <View style={styles.iconBox}>
                            <Feather name="eye-off" size={20} color={colors.accent} />
                        </View>
                        <View style={styles.info}>
                            <Text style={styles.itemTitle}>Product improvement</Text>
                            <Text style={styles.itemSubtitle}>Allow anonymous use of feedback to improve scoring quality</Text>
                        </View>
                        <View style={styles.toggleOn} />
                    </Pressable>

                    <Pressable style={styles.privacyItem}>
                        <View style={styles.iconBox}>
                            <Feather name="clock" size={20} color={colors.accent} />
                        </View>
                        <View style={styles.info}>
                            <Text style={styles.itemTitle}>Data retention</Text>
                            <Text style={styles.itemSubtitle}>Choose how long practice data stays in your account</Text>
                        </View>
                        <View style={styles.toggleOff} />
                    </Pressable>

                    <Pressable style={styles.privacyItem}>
                        <View style={styles.iconBox}>
                            <Feather name="database" size={20} color={colors.accent} />
                        </View>
                        <View style={styles.info}>
                            <Text style={styles.itemTitle}>Export your data</Text>
                            <Text style={styles.itemSubtitle}>Download your session history</Text>
                        </View>
                        <Feather name="download" size={18} color={colors.textMuted} />
                    </Pressable>
                </View>
            </ScrollView>

            <View style={styles.footer}>
                <Pressable
                    onPress={() => router.back()}
                    style={({ pressed }) => [
                        styles.primaryButton,
                        pressed && { opacity: 0.9 }
                    ]}
                >
                    <Text style={styles.buttonText}>Done</Text>
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
    securityCard: {
        backgroundColor: 'rgba(0, 214, 143, 0.05)',
        padding: 24,
        borderRadius: 32,
        borderWidth: 1,
        borderColor: 'rgba(0, 214, 143, 0.15)',
        marginBottom: 40,
    },
    securityHeader: {
        flexDirection: 'row',
        alignItems: 'center',
        gap: 12,
        marginBottom: 8,
    },
    securityTitle: {
        color: colors.success,
        fontSize: 18,
        fontWeight: '900',
    },
    securityText: {
        color: '#FFFFFF',
        fontSize: 14,
        lineHeight: 20,
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
    privacyItem: {
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
    itemTitle: {
        color: '#FFFFFF',
        fontSize: 17,
        fontWeight: '900',
    },
    itemSubtitle: {
        color: colors.textMuted,
        fontSize: 13,
        marginTop: 2,
        opacity: 0.7,
    },
    toggleOn: {
        width: 44,
        height: 24,
        borderRadius: 12,
        backgroundColor: colors.accent,
    },
    toggleOff: {
        width: 44,
        height: 24,
        borderRadius: 12,
        backgroundColor: 'rgba(255,255,255,0.1)',
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
        backgroundColor: colors.card,
        height: 64,
        borderRadius: 32,
        alignItems: 'center',
        justifyContent: 'center',
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.1)',
    },
    buttonText: {
        color: '#FFFFFF',
        fontSize: 18,
        fontWeight: '800',
    },
});
