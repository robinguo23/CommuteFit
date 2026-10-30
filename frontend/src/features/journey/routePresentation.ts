export function isWalkingOnlyRoute(modes: string[]): boolean {
  return modes.length > 0 && modes.every(mode => ['walking', 'walk'].includes(mode.trim().toLowerCase()))
}
