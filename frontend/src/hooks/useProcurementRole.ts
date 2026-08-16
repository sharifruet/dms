import { useAppSelector } from './redux';

/**
 * Who the signed-in user is allowed to be in the procurement module (Q-17, REQ-X4).
 *
 * The Maker captures — creates packages, uploads documents, enters and corrects values.
 * The Checker approves — verifies values, completes stages, marks a stage not applicable,
 * sends it back for rework, re-tenders, and sets budget.
 *
 * The backend enforces this in SecurityConfig; this hook exists so the UI does not offer
 * buttons that will come back 403. Showing an action a user cannot perform is worse than
 * hiding it — they cannot tell a permission problem from a broken feature.
 */
export interface ProcurementRole {
  /** May enter and correct data. */
  canCapture: boolean;
  /** May approve: complete stages, mark not applicable, rework, re-tender, set budget. */
  canApprove: boolean;
  /** Neither — the module should not really be reachable. */
  hasNoAccess: boolean;
  roleName: string;
}

export function useProcurementRole(): ProcurementRole {
  const role = useAppSelector((state) => state.auth.user?.role) ?? '';
  const normalized = role.toUpperCase().replace(/^ROLE_/, '');

  const isAdmin = normalized === 'ADMIN';
  const isChecker = normalized === 'CHECKER';
  const isMaker = normalized === 'MAKER';

  return {
    canCapture: isAdmin || isChecker || isMaker,
    canApprove: isAdmin || isChecker,
    hasNoAccess: !isAdmin && !isChecker && !isMaker,
    roleName: normalized,
  };
}

export default useProcurementRole;
