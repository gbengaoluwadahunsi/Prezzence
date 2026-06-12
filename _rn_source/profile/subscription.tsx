import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Button } from '../../src/components/Button';
import { Card } from '../../src/components/Card';
import { Header } from '../../src/components/Header';
import { Screen } from '../../src/components/Screen';
import { BETA_UNLOCK_ALL_FEATURES } from '../../src/config/featureFlags';
import { trackEvent } from '../../src/services/analytics';
import { useSessionStore } from '../../src/store/sessionStore';
import { colors, spacing, typography } from '../../src/theme';

export default function SubscriptionBillingScreen() {
  const [notice, setNotice] = React.useState('');
  const isPremium = useSessionStore((state) => state.isPremium);
  const hasPremiumAccess = BETA_UNLOCK_ALL_FEATURES || isPremium;
  const planName = hasPremiumAccess ? 'Beta access' : 'Free plan';
  const handleRestore = () => {
    trackEvent('restore_purchases_pressed', { source: 'subscription_settings', billingProvider: 'store_billing_pending' });
    setNotice('There is nothing to restore during beta. When paid plans launch, this will reconnect your App Store or Google Play purchase.');
  };

  return (
    <Screen>
      <Header eyebrow="Subscription and billing" title={planName} subtitle="All features are open during beta testing. Payments will be added later." />
      <Card selected>
        <Text style={styles.title}>Current plan</Text>
        <Text style={styles.body}>
          {hasPremiumAccess
            ? 'Beta access is active. Testers can use advanced interview modes, company research, coaching, and reports.'
            : 'Free access includes core practice, score history, and basic reports.'}
        </Text>
      </Card>
      <Card>
        <Text style={styles.title}>Payments</Text>
        <Text style={styles.body}>No payment is required during beta. Later, purchases and cancellation should happen through Google Play or the App Store.</Text>
      </Card>
      <View style={styles.actions}>
        <Button onPress={handleRestore} variant="secondary">Restore purchases</Button>
        {notice ? <Text style={styles.noticeText}>{notice}</Text> : null}
      </View>
    </Screen>
  );
}

const styles = StyleSheet.create({
  title: { color: colors.text, fontSize: typography.h3, fontWeight: '800', marginBottom: spacing.sm },
  body: { color: colors.textMuted, fontSize: typography.body, lineHeight: 24 },
  actions: { gap: spacing.sm },
  noticeText: { color: colors.textMuted, fontSize: typography.small, lineHeight: 20, textAlign: 'center' },
});
