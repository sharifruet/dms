// Debug utility to check authentication state
export const debugAuth = () => {
  const token = localStorage.getItem('token');
  const user = localStorage.getItem('user');
  
  console.log('=== AUTH DEBUG ===');
  console.log('Token exists:', !!token);
  console.log('Token:', token?.substring(0, 20) + '...');
  console.log('User exists:', !!user);
  console.log('User:', user);
  console.log('=================');
  
  return { token, user };
};

// Add to window for easy debugging
if (typeof window !== 'undefined') {
  (window as any).debugAuth = debugAuth;
}

