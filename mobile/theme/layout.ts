// On native, screen width is already well under this, so the cap never kicks
// in. On web (a real browser window), it stops content — pill inputs
// especially — from stretching edge-to-edge and keeps the side padding
// looking intentional instead of vanishingly thin.
export const maxContentWidth = 480;

export const centeredPage = {
  width: '100%' as const,
  maxWidth: maxContentWidth,
  alignSelf: 'center' as const,
};
