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
import { LinearGradient } from 'expo-linear-gradient';

const Colors = {
    bgPrimary: '#0A0A0F',
    accent: '#6C63FF',
    textSecondary: '#8A8A9A',
    card: '#1C1C2E',
    success: '#00D68F',
    danger: '#FF4757',
    warning: '#FFB347',
    border: '#2A2A3E',
};

function AvatarStageItem({ name, status, color, dimmed }: { name: string, status: string, color: string, dimmed?: boolean }) {
    return (
        <View style={[styles.stageItem, dimmed && { opacity: 0.4 }]}>
            <View style={[styles.avatarRing, { borderColor: color }]}>
                <View style={styles.avatarPlaceholder}>
                    <Feather name="user" size={32} color="#FFFFFF" />
                </View>
            </View>
            <Text style={styles.stageName}>{name}</Text>
            <View style={[styles.stageBadge, { backgroundColor: color + '20' }]}>
                <Text style={[styles.stageBadgeText, { color: color }]}>{status}</Text>
            </View>
        </View>
    );
}

export default function SessionSaveErrorScreen() {
    return (
        <SafeAreaView style={styles.container}>
            <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
                {/* Header */}
                <View style={styles.header}>
                    <Pressable onPress={() => router.back()} style={styles.circleBtn}>
                        <Feather name="chevron-left" size={24} color="#FFFFFF" />
                    </Pressable>
                    <Text style={styles.headerTitle}>Session Failed to Save</Text>
                    <Pressable onPress={() => router.replace('/(tabs)/home' as any)} style={styles.circleBtn}>
                        <Feather name="home" size={22} color="#FFFFFF" />
                    </Pressable>
                </View>

                {/* Progress Tracker */}
                <View style={styles.progressBar}>
                    {[...Array(7)].map((_, i) => (
                        <View
                            key={i}
                            style={[styles.progressSegment, i < 3 && styles.progressSegmentActive]}
                        />
                    ))}
                </View>

                {/* Title */}
                <View style={styles.titleSection}>
                    <Text style={styles.headline}>Session couldn't be saved.</Text>
                    <Text style={styles.subheadline}>We'll keep retrying before you lose anything.</Text>
                </View>

                {/* Stage Hero */}
                <View style={styles.stageHero}>
                    <LinearGradient
                        colors={['rgba(108, 99, 255, 0.1)', 'transparent']}
                        style={styles.stageOverlay}
                    />
                    <View style={styles.stageRow}>
                        <AvatarStageItem name="Sarah" status="success" color={Colors.success} />
                        <AvatarStageItem name="Marcus" status="danger" color={Colors.danger} dimmed />
                        <AvatarStageItem name="Priya" status="warning" color={Colors.warning} />
                    </View>
                </View>

                {/* Action Card */}
                <View style={styles.detailCard}>
                    <Text style={styles.detailTitle}>Careful action</Text>

                    <View style={styles.valueItem}>
                        <View style={styles.iconBox}>
                            <Text style={styles.iconText}>M</Text>
                        </View>
                        <View style={styles.info}>
                            <Text style={styles.itemTitle}>Manual retry</Text>
                            <Text style={styles.itemSubtitle}>Recommended</Text>
                        </View>
                        <View style={[styles.statusBadge, { backgroundColor: Colors.success + '20' }]}>
                            <Text style={[styles.statusText, { color: Colors.success }]}>On</Text>
                        </View>
                    </View>

                    <View style={styles.valueItem}>
                        <View style={styles.iconBox}>
                            <Text style={styles.iconText}>E</Text>
                        </View>
                        <View style={styles.info}>
                            <Text style={styles.itemTitle}>Export audio</Text>
                            <Text style={styles.itemSubtitle}>Recommended</Text>
                        </View>
                        <View style={[styles.statusBadge, { backgroundColor: Colors.success + '20' }]}>
                            <Text style={[styles.statusText, { color: Colors.success }]}>On</Text>
                        </View>
                    </View>
                </View>
            </ScrollView>

            {/* Footer */}
            <View style={styles.footer}>
                <Pressable
                    onPress={() => router.back()}
                    style={({ pressed }) => [
                        styles.primaryButton,
                        pressed && { opacity: 0.9 }
                    ]}
                >
                    <Text style={styles.buttonText}>Manual Retry</Text>
                </Pressable>
            </View>
        </SafeAreaView>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: Colors.bgPrimary,
    },
    scrollContent: {
        paddingBottom: 120,
    },
    header: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'space-between',
        paddingHorizontal: 20,
        height: 80,
    },
    circleBtn: {
        width: 44,
        height: 44,
        borderRadius: 22,
        backgroundColor: 'rgba(28, 28, 46, 0.6)',
        alignItems: 'center',
        justifyContent: 'center',
    },
    headerTitle: {
        color: '#FFFFFF',
        fontSize: 18,
        fontWeight: '800',
    },
    progressBar: {
        flexDirection: 'row',
        paddingHorizontal: 24,
        height: 4,
        marginTop: 8,
        gap: 8,
    },
    progressSegment: {
        flex: 1,
        backgroundColor: '#1C1C2E',
        borderRadius: 2,
    },
    progressSegmentActive: {
        backgroundColor: Colors.accent,
    },
    titleSection: {
        paddingHorizontal: 24,
        marginTop: 32,
        marginBottom: 40,
    },
    headline: {
        color: '#FFFFFF',
        fontSize: 44,
        fontWeight: '900',
        letterSpacing: -1,
        lineHeight: 52,
    },
    subheadline: {
        color: Colors.textSecondary,
        fontSize: 16,
        marginTop: 8,
        lineHeight: 24,
    },
    stageHero: {
        height: 300,
        marginHorizontal: 20,
        backgroundColor: 'rgba(28, 28, 46, 0.3)',
        borderRadius: 32,
        overflow: 'hidden',
        justifyContent: 'center',
        marginBottom: 32,
    },
    stageOverlay: {
        ...StyleSheet.absoluteFillObject,
    },
    stageRow: {
        flexDirection: 'row',
        justifyContent: 'space-around',
        alignItems: 'center',
        paddingHorizontal: 10,
    },
    stageItem: {
        alignItems: 'center',
    },
    avatarRing: {
        width: 90,
        height: 90,
        borderRadius: 45,
        borderWidth: 3,
        padding: 4,
        alignItems: 'center',
        justifyContent: 'center',
        marginBottom: 16,
    },
    avatarPlaceholder: {
        width: '100%',
        height: '100%',
        borderRadius: 40,
        backgroundColor: '#2A2A3E',
        alignItems: 'center',
        justifyContent: 'center',
    },
    stageName: {
        color: '#FFFFFF',
        fontSize: 18,
        fontWeight: '900',
        marginBottom: 6,
    },
    stageBadge: {
        paddingHorizontal: 16,
        paddingVertical: 6,
        borderRadius: 12,
    },
    stageBadgeText: {
        fontSize: 11,
        fontWeight: '900',
    },
    detailCard: {
        backgroundColor: Colors.card,
        borderRadius: 32,
        padding: 24,
        marginHorizontal: 20,
        gap: 16,
    },
    detailTitle: {
        color: '#FFFFFF',
        fontSize: 22,
        fontWeight: '800',
        marginBottom: 8,
    },
    valueItem: {
        flexDirection: 'row',
        alignItems: 'center',
        backgroundColor: 'rgba(255, 255, 255, 0.03)',
        borderRadius: 24,
        padding: 16,
        borderWidth: 1,
        borderColor: 'rgba(255, 255, 255, 0.05)',
    },
    iconBox: {
        width: 48,
        height: 48,
        borderRadius: 16,
        backgroundColor: 'rgba(108, 99, 255, 0.15)',
        alignItems: 'center',
        justifyContent: 'center',
        marginRight: 16,
    },
    iconText: {
        color: Colors.accent,
        fontSize: 18,
        fontWeight: '900',
    },
    info: {
        flex: 1,
    },
    itemTitle: {
        color: '#FFFFFF',
        fontSize: 18,
        fontWeight: '800',
    },
    itemSubtitle: {
        color: Colors.textSecondary,
        fontSize: 13,
        marginTop: 4,
    },
    statusBadge: {
        paddingHorizontal: 12,
        paddingVertical: 6,
        borderRadius: 12,
    },
    statusText: {
        fontSize: 11,
        fontWeight: '800',
    },
    footer: {
        position: 'absolute',
        bottom: 0,
        left: 0,
        right: 0,
        padding: 24,
        paddingBottom: 34,
    },
    primaryButton: {
        backgroundColor: Colors.accent,
        height: 64,
        borderRadius: 32,
        alignItems: 'center',
        justifyContent: 'center',
        shadowColor: Colors.accent,
        shadowOpacity: 0.3,
        shadowRadius: 15,
        elevation: 8,
    },
    buttonText: {
        color: '#FFFFFF',
        fontSize: 18,
        fontWeight: '700',
    },
});
