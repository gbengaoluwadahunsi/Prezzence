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

const Colors = {
  bgPrimary: '#0A0A0F',
  accent: '#6C63FF',
  textSecondary: '#8A8A9A',
  card: '#1C1C2E',
  success: '#00D68F',
  border: '#2A2A3E',
};

function PrivacyItem({
  icon,
  title,
  subtitle,
  badgeColor,
  onPress,
}: {
  icon: string;
  title: string;
  subtitle: string;
  badgeColor: string;
  onPress: () => void;
}) {
  return (
    <Pressable onPress={onPress} style={({ pressed }) => [styles.card, pressed && { opacity: 0.85 }]}>
      <View style={styles.iconBox}>
        <Text style={styles.iconText}>{icon}</Text>
      </View>
      <View style={styles.info}>
        <Text style={styles.cardTitle}>{title}</Text>
        <Text style={styles.cardSubtitle}>{subtitle}</Text>
      </View>
      <View style={[styles.statusBadge, { backgroundColor: badgeColor + '20' }]}>
        <Text style={[styles.statusText, { color: badgeColor }]}>Open</Text>
      </View>
    </Pressable>
  );
}

export default function PrivacySettingsScreen() {
  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        {/* Header */}
        <View style={styles.header}>
          <Pressable onPress={() => router.back()} style={styles.circleBtn}>
            <Feather name="chevron-left" size={24} color="#FFFFFF" />
          </Pressable>
          <Text style={styles.headerTitle}>Privacy Settings</Text>
          <Pressable onPress={() => router.push('/settings' as any)} style={styles.circleBtn}>
            <Feather name="settings" size={22} color="#FFFFFF" />
          </Pressable>
        </View>

        {/* Title */}
        <View style={styles.titleSection}>
          <Text style={styles.headline}>Privacy</Text>
          <Text style={styles.subheadline}>Review legal terms, audio consent, and account controls.</Text>
        </View>

        {/* List */}
        <View style={styles.list}>
          <PrivacyItem
            icon="P"
            title="Privacy Policy"
            subtitle="Audio uploads, AI scoring, retention, and processors"
            badgeColor={Colors.success}
            onPress={() => router.push('/legal/privacy' as any)}
          />
          <PrivacyItem
            icon="T"
            title="Terms of Service"
            subtitle="Subscription, AI feedback, and acceptable use terms"
            badgeColor={Colors.accent}
            onPress={() => router.push('/legal/terms' as any)}
          />
          <PrivacyItem
            icon="A"
            title="Audio and AI consent"
            subtitle="Required before recording interview answers"
            badgeColor={Colors.accent}
            onPress={() => router.push('/onboarding/mic-permission' as any)}
          />
          <PrivacyItem
            icon="R"
            title="Password reset"
            subtitle="Send a secure reset email"
            badgeColor={Colors.accent}
            onPress={() => router.push('/auth/forgot-password' as any)}
          />
          <PrivacyItem
            icon="D"
            title="Delete account"
            subtitle="Remove sessions, scores, events, devices, and auth account"
            badgeColor="#FF4757"
            onPress={() => router.push('/profile/delete-account' as any)}
          />
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
    fontSize: 48,
    fontWeight: '900',
    letterSpacing: -1,
  },
  subheadline: {
    color: Colors.textSecondary,
    fontSize: 16,
    marginTop: 8,
    lineHeight: 24,
  },
  list: {
    paddingHorizontal: 20,
    gap: 16,
  },
  card: {
    backgroundColor: Colors.card,
    borderRadius: 24,
    padding: 20,
    flexDirection: 'row',
    alignItems: 'center',
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
  cardTitle: {
    color: '#FFFFFF',
    fontSize: 18,
    fontWeight: '800',
  },
  cardSubtitle: {
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
