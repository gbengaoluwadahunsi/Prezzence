import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import React from 'react';
import {
  ActivityIndicator,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { deleteSession, getSessions } from '../../src/api/sessions';
import { ConfirmSheet } from '../../src/components/ConfirmSheet';
import { useSessionStore } from '../../src/store/sessionStore';
import { colors } from '../../src/theme';

type SessionRow = {
  id: string;
  title?: string;
  type?: string;
  date?: string;
  score?: number;
  status?: string;
};

export default function SessionsHistoryScreen() {
  const { activeSessionId, resetSession } = useSessionStore();
  const [sessions, setSessions] = React.useState<SessionRow[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [refreshing, setRefreshing] = React.useState(false);
  const [deletingSessionId, setDeletingSessionId] = React.useState<string | null>(null);
  const [pendingDeleteSession, setPendingDeleteSession] = React.useState<SessionRow | null>(null);

  const loadSessions = React.useCallback(async (refresh = false) => {
    try {
      refresh ? setRefreshing(true) : setLoading(true);
      const response = await getSessions();
      setSessions(Array.isArray(response?.sessions) ? response.sessions : []);
    } catch (error) {
      console.warn('[SessionsHistory] Failed to load sessions:', error);
      setSessions([]);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  React.useEffect(() => {
    void loadSessions();
  }, [loadSessions]);

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
    } catch (error) {
      console.warn('[SessionsHistory] Failed to delete session:', error);
    } finally {
      setDeletingSessionId(null);
    }
  }, [activeSessionId, pendingDeleteSession, resetSession]);

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
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} style={styles.circleBtn}>
          <Feather name="chevron-left" size={24} color="#FFFFFF" />
        </Pressable>
        <Text style={styles.headerTitle}>Session History</Text>
        <View style={styles.headerSpacer} />
      </View>

      <ScrollView
        contentContainerStyle={styles.content}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={() => loadSessions(true)}
            tintColor={colors.accent}
          />
        }
      >
        <View style={styles.titleBlock}>
          <Text style={styles.eyebrow}>{sessions.length} SESSIONS</Text>
          <Text style={styles.title}>Every interview</Text>
          <Text style={styles.subtitle}>Open any completed or in-progress session report from your account history.</Text>
        </View>

        {loading ? (
          <View style={styles.emptyState}>
            <ActivityIndicator color={colors.accent} />
            <Text style={styles.emptyTitle}>Loading sessions</Text>
          </View>
        ) : sessions.length ? (
          <View style={styles.list}>
            {sessions.map((item) => (
              <Pressable
                key={item.id}
                onPress={() => router.push(`/sessions/${item.id}` as any)}
                style={({ pressed }) => [styles.sessionCard, pressed && { opacity: 0.84 }]}
              >
                <View style={styles.scoreBadge}>
                  <Text style={styles.scoreValue}>{item.score ?? 0}</Text>
                  <Text style={styles.scoreLabel}>score</Text>
                </View>
                <View style={styles.sessionCopy}>
                  <Text style={styles.sessionTitle} numberOfLines={2}>
                    {item.title || 'Interview Assessment'}
                  </Text>
                  <Text style={styles.sessionMeta} numberOfLines={1}>
                    {[item.type, item.date, item.status].filter(Boolean).join(' / ')}
                  </Text>
                </View>
                <View style={styles.sessionActions}>
                  <Pressable
                    accessibilityRole="button"
                    accessibilityLabel={`Delete ${item.title || 'session'}`}
                    onPress={(event) => {
                      event.stopPropagation();
                      setPendingDeleteSession(item);
                    }}
                    disabled={deletingSessionId === item.id}
                    style={({ pressed }) => [
                      styles.deleteBtn,
                      pressed && { opacity: 0.75 },
                      deletingSessionId === item.id && styles.deleteBtnDisabled,
                    ]}
                  >
                    {deletingSessionId === item.id ? (
                      <ActivityIndicator size="small" color="#FF7A8A" />
                    ) : (
                      <Feather name="trash-2" size={16} color="#FF7A8A" />
                    )}
                  </Pressable>
                  <Feather name="chevron-right" size={20} color={colors.textMuted} />
                </View>
              </Pressable>
            ))}
          </View>
        ) : (
          <View style={styles.emptyState}>
            <View style={styles.emptyIcon}>
              <Feather name="archive" size={26} color={colors.accent} />
            </View>
            <Text style={styles.emptyTitle}>No sessions yet</Text>
            <Text style={styles.emptyText}>Complete your first interview to see reports and trend history here.</Text>
            <Pressable onPress={() => router.push('/onboarding/role' as any)} style={styles.startBtn}>
              <Text style={styles.startText}>Start interview</Text>
            </Pressable>
          </View>
        )}
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    backgroundColor: colors.background,
  },
  header: {
    height: 80,
    paddingHorizontal: 16,
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
  },
  circleBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: 'rgba(28, 28, 46, 0.72)',
    alignItems: 'center',
    justifyContent: 'center',
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  headerTitle: {
    color: '#FFFFFF',
    fontSize: 17,
    fontWeight: '900',
  },
  headerSpacer: {
    width: 44,
  },
  content: {
    paddingHorizontal: 20,
    paddingBottom: 120,
  },
  titleBlock: {
    marginTop: 20,
    marginBottom: 24,
  },
  eyebrow: {
    color: colors.accent,
    fontSize: 11,
    fontWeight: '900',
    letterSpacing: 1.6,
    marginBottom: 8,
  },
  title: {
    color: '#FFFFFF',
    fontSize: 38,
    lineHeight: 44,
    fontWeight: '900',
  },
  subtitle: {
    color: colors.textMuted,
    fontSize: 15,
    lineHeight: 22,
    marginTop: 8,
    fontWeight: '700',
  },
  list: {
    gap: 12,
  },
  sessionCard: {
    minHeight: 96,
    borderRadius: 24,
    padding: 14,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.06)',
  },
  scoreBadge: {
    width: 58,
    height: 58,
    borderRadius: 18,
    backgroundColor: 'rgba(108, 99, 255, 0.14)',
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 14,
  },
  scoreValue: {
    color: '#FFFFFF',
    fontSize: 20,
    fontWeight: '900',
  },
  scoreLabel: {
    color: colors.textMuted,
    fontSize: 9,
    fontWeight: '900',
    textTransform: 'uppercase',
  },
  sessionCopy: {
    flex: 1,
    minWidth: 0,
  },
  sessionTitle: {
    color: '#FFFFFF',
    fontSize: 16,
    lineHeight: 21,
    fontWeight: '900',
  },
  sessionMeta: {
    color: colors.textMuted,
    fontSize: 12,
    fontWeight: '800',
    marginTop: 5,
  },
  sessionActions: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  deleteBtn: {
    width: 40,
    height: 40,
    borderRadius: 20,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(255, 71, 87, 0.12)',
    borderWidth: 1,
    borderColor: 'rgba(255, 71, 87, 0.2)',
  },
  deleteBtnDisabled: {
    opacity: 0.55,
  },
  emptyState: {
    minHeight: 260,
    borderRadius: 28,
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.06)',
    alignItems: 'center',
    justifyContent: 'center',
    padding: 24,
  },
  emptyIcon: {
    width: 58,
    height: 58,
    borderRadius: 20,
    backgroundColor: 'rgba(108, 99, 255, 0.12)',
    alignItems: 'center',
    justifyContent: 'center',
    marginBottom: 14,
  },
  emptyTitle: {
    color: '#FFFFFF',
    fontSize: 17,
    fontWeight: '900',
    marginTop: 10,
  },
  emptyText: {
    color: colors.textMuted,
    textAlign: 'center',
    fontSize: 13,
    lineHeight: 20,
    marginTop: 8,
  },
  startBtn: {
    height: 46,
    borderRadius: 23,
    backgroundColor: colors.accent,
    paddingHorizontal: 22,
    alignItems: 'center',
    justifyContent: 'center',
    marginTop: 18,
  },
  startText: {
    color: '#FFFFFF',
    fontSize: 14,
    fontWeight: '900',
  },
});
