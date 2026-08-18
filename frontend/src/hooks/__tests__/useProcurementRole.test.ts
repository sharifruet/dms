import { renderHook } from '@testing-library/react';
import React from 'react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import authReducer from '../../store/slices/authSlice';
import useProcurementRole from '../useProcurementRole';

/**
 * Who may approve (Q-17, REQ-X4).
 *
 * <p>The server enforces this; the hook exists so the UI does not offer buttons that come
 * back 403, because a permission boundary and a broken feature look identical from the
 * user's side. If this drifts from SecurityConfig, Makers get dead buttons again.
 */
const withRole = (role?: string) => {
  const store = configureStore({
    reducer: { auth: authReducer },
    preloadedState: {
      auth: {
        isAuthenticated: true,
        token: 'test',
        user: role === undefined
          ? null
          : { username: 'someone', role, department: 'BPDB' },
      },
    } as any,
  });

  return renderHook(() => useProcurementRole(), {
    wrapper: ({ children }) => React.createElement(Provider, { store }, children),
  }).result.current;
};

describe('useProcurementRole', () => {
  it('lets a Maker capture but not approve', () => {
    const role = withRole('MAKER');
    expect(role.canCapture).toBe(true);
    expect(role.canApprove).toBe(false);
  });

  it('lets a Checker do both', () => {
    const role = withRole('CHECKER');
    expect(role.canCapture).toBe(true);
    expect(role.canApprove).toBe(true);
  });

  it('lets an Admin do both', () => {
    const role = withRole('ADMIN');
    expect(role.canApprove).toBe(true);
  });

  it('tolerates the ROLE_ prefix Spring Security uses', () => {
    // The token and the database disagree about the prefix depending on where the value
    // came from; the split must not hinge on that
    expect(withRole('ROLE_CHECKER').canApprove).toBe(true);
    expect(withRole('role_maker').canCapture).toBe(true);
  });

  it('gives an unrelated role nothing', () => {
    const role = withRole('VIEWER');
    expect(role.canCapture).toBe(false);
    expect(role.canApprove).toBe(false);
    expect(role.hasNoAccess).toBe(true);
  });

  it('gives a signed-out user nothing rather than throwing', () => {
    const role = withRole(undefined);
    expect(role.hasNoAccess).toBe(true);
  });
});
