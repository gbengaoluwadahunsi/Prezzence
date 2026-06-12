import { router } from 'expo-router';
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { Button } from '../../src/components/Button';
import { Card } from '../../src/components/Card';
import { Header } from '../../src/components/Header';
import { Screen } from '../../src/components/Screen';
import { trackEvent } from '../../src/services/analytics';
import { colors, spacing, typography } from '../../src/theme';

const benefits = [
  'Unlimited sessions',
  'Role skills and leadership practice',
  'More interviewer styles',
  'Interview countdown plan',
  'PDF export for session reports',
  'Online company research',
  'Deeper session reports and trend coaching',
  'Higher usage limits for scoring and voice',
];

export default function PaywallModal() {
  const [notice, setNotice] = React.useState('');

  const handleSubscribe = () => {
    trackEvent('paywall_subscribe_pressed', {
      source: 'paywall',
      billingProvider: 'store_billing_pending',
    });
    setNotice('Payments are not live yet. All beta testers can use the app without paying. Paid plans will open after store checkout is ready.');
  };

  const handleRestore = () => {
    trackEvent('restore_purchases_pressed', {
      source: 'paywall',
      billingProvider: 'store_billing_pending',
    });
    setNotice('There is nothing to restore during beta. When paid plans launch, this will reconnect your App Store or Google Play purchase.');
  };

  return (
    <Screen
      footer={
        <View style={styles.footerActions}>
          <Button onPress={handleSubscribe}>Start Pro</Button>
          <Button onPress={handleRestore} variant="secondary">Restore purchases</Button>
          <Button onPress={() => router.back()} variant="ghost">Not now</Button>
        </View>
      }
    >
      <Header
        eyebrow="Prezzence Pro"
        title="Practice more deeply"
        subtitle="Pro is for candidates who want more sessions, stronger reports, company research, and advanced practice options."
      />
      <Card selected>
        <Text style={styles.price}>Pro</Text>
        <Text style={styles.priceMeta}>Final pricing appears through Google Play or App Store checkout.</Text>
        <View style={styles.benefits}>
          {benefits.map((benefit) => (
            <Text key={benefit} style={styles.benefit}>{benefit}</Text>
          ))}
        </View>
      </Card>
      <Card>
        <Text style={styles.noticeTitle}>Beta access</Text>
        <Text style={styles.noticeText}>
          Payments are switched off while testers use the app. You can explore Pro features and send feedback before pricing goes live.
        </Text>
      </Card>
      {notice ? (
        <Card>
          <Text style={styles.noticeTitle}>Status</Text>
          <Text style={styles.noticeText}>{notice}</Text>
        </Card>
      ) : null}
    </Screen>
  );
}

const styles = StyleSheet.create({
  price: {
    color: colors.text,
    fontSize: 48,
    fontWeight: '900',
    textAlign: 'center',
  },
  priceMeta: {
    color: colors.textMuted,
    fontSize: typography.small,
    textAlign: 'center',
    lineHeight: 20,
  },
  benefits: {
    gap: spacing.md,
    marginTop: spacing.lg,
  },
  benefit: {
    color: colors.text,
    fontSize: typography.body,
    fontWeight: '700',
  },
  noticeTitle: {
    color: colors.text,
    fontSize: typography.body,
    fontWeight: '900',
    marginBottom: spacing.sm,
  },
  noticeText: {
    color: colors.textMuted,
    fontSize: typography.small,
    lineHeight: 20,
    fontWeight: '600',
  },
  footerActions: {
    gap: spacing.sm,
  },
});
