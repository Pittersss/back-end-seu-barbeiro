// On native, screen width is already well under this, so the cap never kicks
// in. On web (a real browser window) it holds the content in a single centred
// column — the app-like reading measure modern product sites use — instead of
// letting RN's flexible-width rows stretch edge-to-edge on a desktop monitor.
export const maxContentWidth = 520;

export const centeredPage = {
  width: '100%' as const,
  maxWidth: maxContentWidth,
  alignSelf: 'center' as const,
};

// Slightly wider measure for full-bleed hero / media sections that can breathe
// more than a form column.
export const maxWideWidth = 640;

export const widePage = {
  width: '100%' as const,
  maxWidth: maxWideWidth,
  alignSelf: 'center' as const,
};
