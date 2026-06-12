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

import { ConfirmSheet } from '../../src/components/ConfirmSheet';
import { deleteNotification, getNotifications, markNotificationRead } from '../../src/api/users';
import { colors } from '../../src/theme';

type Notification = {
  id: string;
  title?: string;
  message?: string;
  type?: string;
  is_read?: boolean;
  created_at?: string;
};

function iconForType(type?: string): keyof typeof Feather.glyphMap {
  switch ((type || '').toLowerCase()) {
    case 'score':
      return 'bar-chart-2';
    case 'streak':
      return 'award';
    case 'payment':
      return 'credit-card';
    case 'warning':
      return 'alert-triangle';
    default:
      return 'bell';
  }
}

function formatTime(value?: string) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return '';
  return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric' });
}

function NotificationRow({
  item,
  onRead,
  onDelete,
  deleting,
}: {
  item: Notification;
  onRead: (id: string) => void;
  onDelete: (item: Notification) => void;
  deleting: boolean;
}) {
  const unread = !item.is_read;
  return (
    <Pressable
      onPress={() => onRead(item.id)}
      style={[styles.notificationCard, unread && styles.unreadCard]}
    >
      <View style={[styles.iconBox, unread && styles.unreadIconBox]}>
        <Feather name={iconForType(item.type)} size={20} color={unread ? '#FFFFFF' : colors.accent} />
      </View>
      <View style={styles.notificationBody}>
        <View style={styles.notificationHeader}>
          <Text style={styles.notificationTitle} numberOfLines={1}>
            {item.title || 'Notification'}
          </Text>
          <Text style={styles.notificationTime}>{formatTime(item.created_at)}</Text>
        </View>
        <Text style={styles.notificationMessage} numberOfLines={3}>
          {item.message || 'Open Prezzence for your latest practice update.'}
        </Text>
      </View>
      {unread ? <View style={styles.unreadDot} /> : null}
      <Pressable
        onPress={(event) => {
          event.stopPropagation();
          onDelete(item);
        }}
        disabled={deleting}
        hitSlop={10}
        style={({ pressed }) => [
          styles.deleteBtn,
          deleting && styles.deleteBtnDisabled,
          pressed && { opacity: 0.72 },
        ]}
        accessibilityRole="button"
        accessibilityLabel="Delete notification"
      >
        {deleting ? (
          <ActivityIndicator size="small" color="#FF8A9A" />
        ) : (
          <Feather name="trash-2" size={16} color="#FF8A9A" />
        )}
      </Pressable>
    </Pressable>
  );
}

export default function NotificationsScreen() {
  const [notifications, setNotifications] = React.useState<Notification[]>([]);
  const [loading, setLoading] = React.useState(true);
  const [refreshing, setRefreshing] = React.useState(false);
  const [deletingId, setDeletingId] = React.useState<string | null>(null);
  const [pendingDelete, setPendingDelete] = React.useState<Notification | null>(null);

  const loadNotifications = React.useCallback(async (refresh = false) => {
    try {
      refresh ? setRefreshing(true) : setLoading(true);
      const rows = await getNotifications();
      setNotifications(Array.isArray(rows) ? rows : []);
    } catch (error) {
      console.warn('[Notifications] Failed to load notifications:', error);
      setNotifications([]);
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  React.useEffect(() => {
    void loadNotifications();
  }, [loadNotifications]);

  const handleRead = async (id: string) => {
    setNotifications((items) =>
      items.map((item) => item.id === id ? { ...item, is_read: true } : item)
    );
    try {
      await markNotificationRead(id);
    } catch (error) {
      console.warn('[Notifications] Failed to mark read:', error);
    }
  };

  const handleDelete = (item: Notification) => {
    setPendingDelete(item);
  };

  const confirmDelete = async () => {
    if (!pendingDelete) return;
    const previous = notifications;
    setDeletingId(pendingDelete.id);
    setNotifications((items) => items.filter((row) => row.id !== pendingDelete.id));
    try {
      await deleteNotification(pendingDelete.id);
      setPendingDelete(null);
    } catch (error) {
      console.warn('[Notifications] Failed to delete notification:', error);
      setNotifications(previous);
    } finally {
      setDeletingId(null);
    }
  };

  const unreadCount = notifications.filter((item) => !item.is_read).length;

  return (
    <SafeAreaView style={styles.container}>
      <ConfirmSheet
        visible={Boolean(pendingDelete)}
        title="Delete notification?"
        message={pendingDelete?.title || 'This notification will be removed from your inbox.'}
        confirmLabel="Delete"
        danger
        loading={Boolean(pendingDelete && deletingId === pendingDelete.id)}
        onCancel={() => {
          if (!deletingId) setPendingDelete(null);
        }}
        onConfirm={confirmDelete}
      />
      <View style={styles.header}>
        <Pressable onPress={() => router.back()} style={styles.circleBtn}>
          <Feather name="chevron-left" size={24} color="#FFFFFF" />
        </Pressable>
        <Text style={styles.headerTitle}>Notifications</Text>
        <Pressable onPress={() => router.push('/settings/notifications' as any)} style={styles.circleBtn}>
          <Feather name="sliders" size={20} color="#FFFFFF" />
        </Pressable>
      </View>

      <ScrollView
        contentContainerStyle={styles.scrollContent}
        showsVerticalScrollIndicator={false}
        refreshControl={
          <RefreshControl
            refreshing={refreshing}
            onRefresh={() => loadNotifications(true)}
            tintColor={colors.accent}
          />
        }
      >
        <View style={styles.titleSection}>
          <Text style={styles.eyebrow}>{unreadCount ? `${unreadCount} UNREAD` : 'ALL CAUGHT UP'}</Text>
          <Text style={styles.headline}>Alerts</Text>
          <Text style={styles.subheadline}>
            Practice reminders, score updates, milestones, and account notices.
          </Text>
        </View>

        {loading ? (
          <View style={styles.emptyState}>
            <ActivityIndicator color={colors.accent} />
            <Text style={styles.emptyTitle}>Loading notifications</Text>
          </View>
        ) : notifications.length ? (
          <View style={styles.list}>
            {notifications.map((item) => (
              <NotificationRow
                key={item.id}
                item={item}
                onRead={handleRead}
                onDelete={handleDelete}
                deleting={deletingId === item.id}
              />
            ))}
          </View>
        ) : (
          <View style={styles.emptyState}>
            <View style={styles.emptyIcon}>
              <Feather name="bell-off" size={26} color={colors.accent} />
            </View>
            <Text style={styles.emptyTitle}>No notifications yet</Text>
            <Text style={styles.emptyText}>
              You will see practice reminders, milestones, and score alerts here.
            </Text>
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
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingHorizontal: 16,
    height: 80,
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
  scrollContent: {
    paddingBottom: 120,
  },
  titleSection: {
    paddingHorizontal: 24,
    marginTop: 16,
    marginBottom: 24,
  },
  eyebrow: {
    color: colors.accent,
    fontSize: 11,
    fontWeight: '900',
    letterSpacing: 1.6,
    marginBottom: 8,
  },
  headline: {
    color: '#FFFFFF',
    fontSize: 42,
    fontWeight: '900',
    letterSpacing: -1,
  },
  subheadline: {
    color: colors.textMuted,
    fontSize: 15,
    lineHeight: 22,
    marginTop: 8,
  },
  list: {
    paddingHorizontal: 20,
    gap: 12,
  },
  notificationCard: {
    minHeight: 86,
    borderRadius: 22,
    padding: 14,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
  },
  unreadCard: {
    borderColor: 'rgba(108, 99, 255, 0.34)',
    backgroundColor: 'rgba(108, 99, 255, 0.12)',
  },
  iconBox: {
    width: 46,
    height: 46,
    borderRadius: 16,
    alignItems: 'center',
    justifyContent: 'center',
    marginRight: 13,
    backgroundColor: 'rgba(108, 99, 255, 0.14)',
  },
  unreadIconBox: {
    backgroundColor: colors.accent,
  },
  notificationBody: {
    flex: 1,
    minWidth: 0,
  },
  notificationHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  notificationTitle: {
    flex: 1,
    color: '#FFFFFF',
    fontSize: 15,
    fontWeight: '900',
  },
  notificationTime: {
    color: colors.textMuted,
    fontSize: 11,
    fontWeight: '800',
  },
  notificationMessage: {
    color: colors.textMuted,
    fontSize: 13,
    lineHeight: 19,
    fontWeight: '600',
    marginTop: 4,
  },
  unreadDot: {
    width: 8,
    height: 8,
    borderRadius: 4,
    backgroundColor: '#FF4757',
    marginLeft: 10,
  },
  deleteBtn: {
    width: 36,
    height: 36,
    borderRadius: 18,
    marginLeft: 8,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: 'rgba(255, 71, 87, 0.1)',
    borderWidth: 1,
    borderColor: 'rgba(255, 71, 87, 0.16)',
  },
  deleteBtnDisabled: {
    opacity: 0.6,
  },
  emptyState: {
    marginHorizontal: 20,
    minHeight: 220,
    borderRadius: 26,
    backgroundColor: colors.card,
    borderWidth: 1,
    borderColor: 'rgba(255, 255, 255, 0.05)',
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
});
