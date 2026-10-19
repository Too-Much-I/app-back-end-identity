export function tokenPair(result) {
  if (!result || typeof result.accessToken !== 'string' || !result.accessToken ||
      typeof result.refreshToken !== 'string' || !result.refreshToken) throw new Error('INVALID_TOKEN_RESPONSE');
  return { accessToken: result.accessToken, refreshToken: result.refreshToken };
}
export function claimSummary(token) {
  const payload = JSON.parse(new TextDecoder().decode(Uint8Array.from(atob(token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/')), c => c.charCodeAt(0))));
  return { subIsUuid: /^[0-9a-f]{8}(-[0-9a-f]{4}){3}-[0-9a-f]{12}$/i.test(payload.sub),
    member: payload.account_type === 'MEMBER', learningAudience: Array.isArray(payload.aud) ? payload.aud.includes('tosunsaeng-learning-core') : payload.aud === 'tosunsaeng-learning-core',
    expiresAt: Number.isFinite(payload.exp) ? new Date(payload.exp * 1000).toISOString() : null };
}
