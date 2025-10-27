import React from 'react';
import { Container, Typography, Box } from '@mui/material';

const Users: React.FC = () => {
  return (
    <Container maxWidth="lg">
      <Box sx={{ mb: 4 }}>
        <Typography variant="h4" component="h1" gutterBottom>
          Users
        </Typography>
        <Typography variant="body1" color="text.secondary">
          User management functionality will be implemented in Phase 2.
        </Typography>
      </Box>
    </Container>
  );
};

export default Users;
