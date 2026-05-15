
import type OlMap from '@/components/OlMap/OlMap'

declare global {
  interface Window {
    olMap: OlMap | null;
    $bus: any;
    hasPermission: (permission: string) => boolean
  }

  const olMap: OlMap | null
}
