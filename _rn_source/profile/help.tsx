import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import React from 'react';
import {
  ScrollView,
  StyleSheet,
  Text,
  View,
  Pressable,
  TextInput
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

function HelpItem({ icon, title, subtitle, badgeColor }: { icon: string, title: string, subtitle: string, badgeColor: string }) {
  return (
    <View style={styles.card}>
      <View style={styles.iconBox}>
        <Text style={styles.iconText}>{icon}</Text>
      </View>
      <View style={styles.info}>
        <Text style={styles.cardTitle}>{title}</Text>
        <Text style={styles.cardSubtitle}>{subtitle}</Text>
      </View>
      <View style={[styles.statusBadge, { backgroundColor: badgeColor + '20' }]}>
        <Text style={[styles.statusText, { color: badgeColor }]}>On</Text>
      </View>
    </View>
  );
}

export default function HelpScreen() {
  return (
    <SafeAreaView style={styles.container}>
      <ScrollView contentContainerStyle={styles.scrollContent} showsVerticalScrollIndicator={false}>
        {/* Header */}
        <View style={styles.header}>
          <Pressable onPress={() => router.back()} style={styles.circleBtn}>
            <Feather name="chevron-left" size={24} color="#FFFFFF" />
          </Pressable>
          <Text style={styles.headerTitle}>Help & FAQ</Text>
          <Pressable onPress={() => router.push('/feedback' as any)} style={styles.circleBtn}>
            <Feather name="message-circle" size={22} color="#FFFFFF" />
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
          <Text style={styles.headline}>Help</Text>
          <Text style={styles.subheadline}>Search support topics and tutorials.</Text>
        </View>

        {/* Search */}
        <View style={styles.searchWrapper}>
          <Text style={styles.searchLabel}>SEARCH HELP</Text>
          <View style={styles.searchInputBox}>
            <TextInput
              placeholder="Scoring, subscriptions, microphone..."
              placeholderTextColor="rgba(255,255,255,0.3)"
              style={styles.textInput}
            />
          </View>
        </View>

        {/* List */}
        <View style={styles.list}>
          <HelpItem
            icon="G"
            title="Getting Started"
            subtitle="Recommended"
            badgeColor={Colors.success}
          />
          <HelpItem
            icon="I"
            title="Interview Modes"
            subtitle="Ready"
            badgeColor={Colors.accent}
          />
          <HelpItem
            icon="S"
            title="Scoring & Feedback"
            subtitle="Ready"
            badgeColor={Colors.accent}
          />
          <HelpItem
            icon="A"
            title="Avatar Interviews"
            subtitle="Animated preview"
            badgeColor={Colors.accent}
          />
        </View>
      </ScrollView>

      {/* Footer */}
      <View style={styles.footer}>
        <Pressable
          onPress={() => router.push('/feedback' as any)}
          style={({ pressed }) => [
            styles.primaryButton,
            pressed && { opacity: 0.9 }
          ]}
        >
          <Text style={styles.buttonText}>Contact Support</Text>
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
  searchWrapper: {
    paddingHorizontal: 20,
    marginBottom: 32,
  },
  searchLabel: {
    color: Colors.textSecondary,
    fontSize: 10,
    fontWeight: '900',
    letterSpacing: 2,
    marginBottom: 12,
  },
  searchInputBox: {
    backgroundColor: Colors.card,
    height: 64,
    borderRadius: 16,
    paddingHorizontal: 20,
    justifyContent: 'center',
  },
  textInput: {
    color: '#FFFFFF',
    fontSize: 16,
    fontWeight: '600',
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
