import type { ReactNode } from 'react';
import { KeyboardAvoidingView, Platform, ScrollView, View } from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';

import { centeredPage } from '../theme/layout';
import { spacing } from '../theme/spacing';
import { useThemedStyles } from '../theme/useThemedStyles';

interface ScreenProps {
  children: ReactNode;
  scroll?: boolean;
  center?: boolean;
  /** Rendered pinned to the top-right corner, above everything else — for a
   * control that must stay visible regardless of scroll position or `center`
   * (e.g. the login screen's theme toggle). */
  topRight?: ReactNode;
}

export function Screen({ children, scroll = true, center = false, topRight }: ScreenProps) {
  const styles = useThemedStyles((colors) => ({
    safe: {
      flex: 1,
      backgroundColor: colors.bg,
    },
    flex: {
      flex: 1,
    },
    scrollContent: {
      flexGrow: 1,
    },
    content: {
      flex: 1,
      paddingHorizontal: spacing.lg,
      paddingVertical: spacing.xl,
      ...centeredPage,
    },
    center: {
      justifyContent: 'center',
    },
    topRight: {
      position: 'absolute',
      top: spacing.md,
      right: spacing.lg,
      zIndex: 10,
    },
  }));

  const content = (
    <View style={[styles.content, center && styles.center]}>{children}</View>
  );

  return (
    <SafeAreaView style={styles.safe} edges={['top', 'bottom']}>
      {topRight ? <View style={styles.topRight}>{topRight}</View> : null}
      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        {scroll ? (
          <ScrollView
            contentContainerStyle={styles.scrollContent}
            keyboardShouldPersistTaps="handled"
          >
            {content}
          </ScrollView>
        ) : (
          content
        )}
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
}
