import React, { useEffect, useMemo, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  CardHeader,
  Checkbox,
  CircularProgress,
  Container,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Divider,
  FormControl,
  FormControlLabel,
  FormGroup,
  Grid,
  IconButton,
  InputLabel,
  List,
  ListItemButton,
  ListItemText,
  MenuItem,
  Select,
  SelectChangeEvent,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TablePagination,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import AddIcon from '@mui/icons-material/Add';
import EditIcon from '@mui/icons-material/Edit';

import { Permission, Role, UserDetail } from '../types/userManagement';
import {
  CreateUserPayload,
  UpdateRolePayload,
  UpdateUserPayload,
  userManagementService,
} from '../services/userManagementService';

interface FeedbackMessage {
  type: 'success' | 'error';
  message: string;
}

const initialCreateForm: CreateUserPayload = {
  username: '',
  email: '',
  password: '',
  firstName: '',
  lastName: '',
  department: '',
  roleId: 0,
};

const Users: React.FC = () => {
  const [users, setUsers] = useState<UserDetail[]>([]);
  const [usersLoading, setUsersLoading] = useState(false);
  const [usersError, setUsersError] = useState<string | null>(null);
  const [page, setPage] = useState(0);
  const [rowsPerPage, setRowsPerPage] = useState(10);
  const [totalUsers, setTotalUsers] = useState(0);

  const [roles, setRoles] = useState<Role[]>([]);
  const [permissions, setPermissions] = useState<Permission[]>([]);
  const [rolesLoading, setRolesLoading] = useState(false);
  const [rolesError, setRolesError] = useState<string | null>(null);

  const [selectedRoleId, setSelectedRoleId] = useState<number | null>(null);
  const [roleForm, setRoleForm] = useState<UpdateRolePayload>({
    displayName: '',
    description: '',
    isActive: true,
  });
  const [rolePermissionIds, setRolePermissionIds] = useState<number[]>([]);
  const [savingRoleMeta, setSavingRoleMeta] = useState(false);
  const [savingRolePermissions, setSavingRolePermissions] = useState(false);

  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [userForm, setUserForm] = useState<CreateUserPayload>(initialCreateForm);
  const [editingUser, setEditingUser] = useState<UserDetail | null>(null);
  const [savingUser, setSavingUser] = useState(false);

  const [feedback, setFeedback] = useState<FeedbackMessage | null>(null);

  const selectedRole = useMemo(
    () => roles.find((role) => role.id === selectedRoleId) || null,
    [roles, selectedRoleId]
  );

  const loadUsers = async (pageNumber = page, pageSize = rowsPerPage) => {
    setUsersLoading(true);
    setUsersError(null);
    try {
      const response = await userManagementService.getUsers({
        page: pageNumber,
        size: pageSize,
        sortBy: 'username',
        sortDir: 'asc',
      });
      setUsers(response.content);
      setTotalUsers(response.totalElements);
    } catch (error) {
      setUsersError(
        error instanceof Error ? error.message : 'Failed to load users.'
      );
    } finally {
      setUsersLoading(false);
    }
  };

  const loadRolesAndPermissions = async () => {
    setRolesLoading(true);
    setRolesError(null);
    try {
      const [rolesResponse, permissionsResponse] = await Promise.all([
        userManagementService.getRoles(),
        userManagementService.getPermissions(),
      ]);
      setRoles(rolesResponse);
      setPermissions(permissionsResponse);

      const initialRoleId =
        selectedRoleId ?? (rolesResponse.length > 0 ? rolesResponse[0].id : null);
      setSelectedRoleId(initialRoleId);

      if (initialRoleId) {
        const role = rolesResponse.find((r) => r.id === initialRoleId);
        if (role) {
          setRoleForm({
            displayName: role.displayName ?? '',
            description: role.description ?? '',
            isActive: role.isActive ?? true,
          });
          setRolePermissionIds(role.permissions.map((perm) => perm.id));
        }
      } else {
        setRoleForm({
          displayName: '',
          description: '',
          isActive: true,
        });
        setRolePermissionIds([]);
      }
    } catch (error) {
      setRolesError(
        error instanceof Error
          ? error.message
          : 'Failed to load roles and permissions.'
      );
    } finally {
      setRolesLoading(false);
    }
  };

  useEffect(() => {
    loadUsers();
  }, [page, rowsPerPage]);

  useEffect(() => {
    loadRolesAndPermissions();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!selectedRoleId) {
      return;
    }
    const role = roles.find((r) => r.id === selectedRoleId);
    if (role) {
      setRoleForm({
        displayName: role.displayName ?? '',
        description: role.description ?? '',
        isActive: role.isActive ?? true,
      });
      setRolePermissionIds(role.permissions.map((perm) => perm.id));
    }
  }, [roles, selectedRoleId]);

  const handleChangePage = (
    _: React.MouseEvent<HTMLButtonElement> | null,
    newPage: number
  ) => {
    setPage(newPage);
  };

  const handleChangeRowsPerPage = (event: React.ChangeEvent<HTMLInputElement>) => {
    setRowsPerPage(parseInt(event.target.value, 10));
    setPage(0);
  };

  const handleOpenCreateDialog = () => {
    setUserForm({
      ...initialCreateForm,
      roleId: roles[0]?.id ?? 0,
    });
    setCreateDialogOpen(true);
  };

  const handleCloseCreateDialog = () => {
    setCreateDialogOpen(false);
    setUserForm(initialCreateForm);
  };

  const handleOpenEditDialog = (user: UserDetail) => {
    setEditingUser(user);
    setUserForm({
      username: user.username,
      email: user.email,
      password: '',
      firstName: user.firstName ?? '',
      lastName: user.lastName ?? '',
      department: user.department ?? '',
      roleId: user.role?.id ?? 0,
    });
    setEditDialogOpen(true);
  };

  const handleCloseEditDialog = () => {
    setEditDialogOpen(false);
    setEditingUser(null);
  };

  const handleUserFormChange = (
    event:
      | React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
      | SelectChangeEvent<number>
  ) => {
    const { name, value } = event.target as HTMLInputElement;
    setUserForm((prev) => ({
      ...prev,
      [name]: name === 'roleId' ? Number(value) : value,
    }));
  };

  const handleCreateUser = async () => {
    setSavingUser(true);
    try {
      const response = await userManagementService.createUser(userForm);
      if (response.success) {
        setFeedback({
          type: 'success',
          message: response.message ?? 'User created successfully.',
        });
        handleCloseCreateDialog();
        loadUsers();
      } else {
        setFeedback({
          type: 'error',
          message: response.message ?? 'Failed to create user.',
        });
      }
    } catch (error) {
      setFeedback({
        type: 'error',
        message:
          error instanceof Error ? error.message : 'Failed to create user.',
      });
    } finally {
      setSavingUser(false);
    }
  };

  const handleUpdateUser = async () => {
    if (!editingUser) {
      return;
    }
    setSavingUser(true);
    try {
      const payload: UpdateUserPayload = {
        firstName: userForm.firstName,
        lastName: userForm.lastName,
        email: userForm.email,
        department: userForm.department,
        roleId: userForm.roleId,
      };
      const response = await userManagementService.updateUser(
        editingUser.id,
        payload
      );
      if (response.success && response.user) {
        setFeedback({
          type: 'success',
          message: response.message ?? 'User updated successfully.',
        });
        setUsers((prev) =>
          prev.map((user) =>
            user.id === response.user?.id ? response.user : user
          )
        );
        handleCloseEditDialog();
      } else {
        setFeedback({
          type: 'error',
          message: response.message ?? 'Failed to update user.',
        });
      }
    } catch (error) {
      setFeedback({
        type: 'error',
        message:
          error instanceof Error ? error.message : 'Failed to update user.',
      });
    } finally {
      setSavingUser(false);
    }
  };

  const handleToggleUserStatus = async (user: UserDetail) => {
    try {
      const response = await userManagementService.toggleUserStatus(user.id);
      if (response.success) {
        setFeedback({
          type: 'success',
          message: response.message ?? 'User status updated.',
        });
        setUsers((prev) =>
          prev.map((item) =>
            item.id === user.id ? { ...item, isActive: !item.isActive } : item
          )
        );
      } else {
        setFeedback({
          type: 'error',
          message: response.message ?? 'Failed to update user status.',
        });
      }
    } catch (error) {
      setFeedback({
        type: 'error',
        message:
          error instanceof Error ? error.message : 'Failed to update user status.',
      });
    }
  };

  const handleRoleSelection = (roleId: number) => {
    setSelectedRoleId(roleId);
  };

  const handleRoleFormChange = (
    event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>
  ) => {
    const { name, value, type } = event.target;
    const checked = (event.target as HTMLInputElement).checked;
    setRoleForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value,
    }));
  };

  const handlePermissionToggle = (permissionId: number) => {
    setRolePermissionIds((prev) =>
      prev.includes(permissionId)
        ? prev.filter((id) => id !== permissionId)
        : [...prev, permissionId]
    );
  };

  const handleSaveRoleMeta = async () => {
    if (!selectedRoleId) {
      return;
    }
    setSavingRoleMeta(true);
    try {
      const updatedRole = await userManagementService.updateRole(
        selectedRoleId,
        roleForm
      );
      setRoles((prev) =>
        prev.map((role) => (role.id === updatedRole.id ? updatedRole : role))
      );
      setFeedback({ type: 'success', message: 'Role details updated.' });
    } catch (error) {
      setFeedback({
        type: 'error',
        message:
          error instanceof Error ? error.message : 'Failed to update role.',
      });
    } finally {
      setSavingRoleMeta(false);
    }
  };

  const handleSaveRolePermissions = async () => {
    if (!selectedRoleId) {
      return;
    }
    setSavingRolePermissions(true);
    try {
      const updatedRole = await userManagementService.updateRolePermissions(
        selectedRoleId,
        rolePermissionIds
      );
      setRoles((prev) =>
        prev.map((role) => (role.id === updatedRole.id ? updatedRole : role))
      );
      setFeedback({
        type: 'success',
        message: 'Role permissions updated.',
      });
    } catch (error) {
      setFeedback({
        type: 'error',
        message:
          error instanceof Error
            ? error.message
            : 'Failed to update role permissions.',
      });
    } finally {
      setSavingRolePermissions(false);
    }
  };

  const renderUsersTable = () => {
    if (usersLoading) {
      return (
        <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
          <CircularProgress />
        </Box>
      );
    }

    if (usersError) {
      return <Alert severity="error">{usersError}</Alert>;
    }

    if (users.length === 0) {
      return (
        <Typography variant="body1" color="text.secondary">
          No users found. Create a new user to get started.
        </Typography>
      );
    }

    return (
      <>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Username</TableCell>
              <TableCell>Name</TableCell>
              <TableCell>Email</TableCell>
              <TableCell>Role</TableCell>
              <TableCell>Department</TableCell>
              <TableCell>Status</TableCell>
              <TableCell align="right">Actions</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {users.map((user) => (
              <TableRow key={user.id} hover>
                <TableCell>{user.username}</TableCell>
                <TableCell>
                  {[user.firstName, user.lastName].filter(Boolean).join(' ')}
                </TableCell>
                <TableCell>{user.email}</TableCell>
                <TableCell>{user.role?.displayName ?? user.role?.name}</TableCell>
                <TableCell>{user.department ?? '—'}</TableCell>
                <TableCell>
                  <Switch
                    size="small"
                    checked={Boolean(user.isActive)}
                    onChange={() => handleToggleUserStatus(user)}
                    color="primary"
                  />
                </TableCell>
                <TableCell align="right">
                  <IconButton
                    aria-label="edit"
                    size="small"
                    onClick={() => handleOpenEditDialog(user)}
                  >
                    <EditIcon fontSize="small" />
                  </IconButton>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
        <TablePagination
          component="div"
          count={totalUsers}
          page={page}
          onPageChange={handleChangePage}
          rowsPerPage={rowsPerPage}
          onRowsPerPageChange={handleChangeRowsPerPage}
          rowsPerPageOptions={[5, 10, 20, 50]}
        />
      </>
    );
  };

  const renderRolePermissions = () => {
    if (!selectedRole) {
      return (
        <Typography variant="body2" color="text.secondary">
          Select a role to view its permissions.
        </Typography>
      );
    }

    const permissionsByResource = permissions.reduce<
      Record<string, Permission[]>
    >((acc, permission) => {
      const key = permission.resource ?? 'GENERAL';
      if (!acc[key]) {
        acc[key] = [];
      }
      acc[key].push(permission);
      return acc;
    }, {});

    return (
      <Box>
        {Object.entries(permissionsByResource).map(([resource, perms]) => (
          <Box key={resource} sx={{ mb: 2 }}>
            <Typography variant="subtitle2" sx={{ mb: 1 }}>
              {resource}
            </Typography>
            <FormGroup>
              {perms.map((permission) => (
                <FormControlLabel
                  key={permission.id}
                  control={
                    <Checkbox
                      checked={rolePermissionIds.includes(permission.id)}
                      onChange={() => handlePermissionToggle(permission.id)}
                      color="primary"
                    />
                  }
                  label={permission.displayName ?? permission.name}
                />
              ))}
            </FormGroup>
            <Divider sx={{ mt: 2 }} />
          </Box>
        ))}
      </Box>
    );
  };

  return (
    <Container maxWidth="lg" sx={{ pb: 6 }}>
      <Box sx={{ mb: 4, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="h4" component="h1">
          User & Role Administration
        </Typography>
        <Box>
          <Button
            variant="contained"
            color="primary"
            startIcon={<AddIcon />}
            onClick={handleOpenCreateDialog}
            sx={{ mr: 1 }}
            disabled={rolesLoading || roles.length === 0}
          >
            New User
          </Button>
          <IconButton aria-label="refresh" onClick={() => loadUsers()} color="primary">
            <RefreshIcon />
          </IconButton>
        </Box>
      </Box>

      {feedback && (
        <Alert
          severity={feedback.type}
          onClose={() => setFeedback(null)}
          sx={{ mb: 3 }}
        >
          {feedback.message}
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid item xs={12}>
          <Card>
            <CardHeader title="Users" />
            <CardContent>{renderUsersTable()}</CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={4}>
          <Card>
            <CardHeader title="Roles" />
            <CardContent>
              {rolesLoading ? (
                <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
                  <CircularProgress size={24} />
                </Box>
              ) : rolesError ? (
                <Alert severity="error">{rolesError}</Alert>
              ) : (
                <List dense>
                  {roles.map((role) => (
                    <ListItemButton
                      key={role.id}
                      selected={role.id === selectedRoleId}
                      onClick={() => handleRoleSelection(role.id)}
                    >
                      <ListItemText
                        primary={role.displayName ?? role.name}
                        secondary={role.description}
                      />
                    </ListItemButton>
                  ))}
                </List>
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid item xs={12} md={8}>
          <Card>
            <CardHeader title="Role Details & Permissions" />
            <CardContent>
              {!selectedRole ? (
                <Typography variant="body2" color="text.secondary">
                  Select a role to manage its details and permissions.
                </Typography>
              ) : (
                <Grid container spacing={3}>
                  <Grid item xs={12} md={6}>
                    <TextField
                      label="Display Name"
                      name="displayName"
                      value={roleForm.displayName ?? ''}
                      onChange={handleRoleFormChange}
                      fullWidth
                      margin="normal"
                    />
                    <TextField
                      label="Description"
                      name="description"
                      value={roleForm.description ?? ''}
                      onChange={handleRoleFormChange}
                      fullWidth
                      multiline
                      minRows={3}
                      margin="normal"
                    />
                    <FormControlLabel
                      control={
                        <Switch
                          name="isActive"
                          checked={roleForm.isActive ?? true}
                          onChange={handleRoleFormChange}
                          color="primary"
                        />
                      }
                      label="Role is active"
                    />
                    <Box sx={{ mt: 2 }}>
                      <Button
                        variant="contained"
                        color="primary"
                        onClick={handleSaveRoleMeta}
                        disabled={savingRoleMeta}
                      >
                        {savingRoleMeta ? 'Saving...' : 'Save Role Details'}
                      </Button>
                    </Box>
                  </Grid>
                  <Grid item xs={12} md={6}>
                    <Typography variant="subtitle1" sx={{ mb: 2 }}>
                      Permissions
                    </Typography>
                    <Box
                      sx={{
                        maxHeight: 320,
                        overflowY: 'auto',
                        pr: 1,
                      }}
                    >
                      {permissions.length === 0 ? (
                        <Typography variant="body2" color="text.secondary">
                          No permissions available.
                        </Typography>
                      ) : (
                        renderRolePermissions()
                      )}
                    </Box>
                    <Box sx={{ mt: 2 }}>
                      <Button
                        variant="outlined"
                        color="primary"
                        onClick={handleSaveRolePermissions}
                        disabled={savingRolePermissions}
                      >
                        {savingRolePermissions ? 'Saving...' : 'Save Permissions'}
                      </Button>
                    </Box>
                  </Grid>
                </Grid>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      <Dialog open={createDialogOpen} onClose={handleCloseCreateDialog} fullWidth maxWidth="sm">
        <DialogTitle>Create User</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <TextField
                label="Username"
                name="username"
                value={userForm.username}
                onChange={handleUserFormChange}
                fullWidth
                required
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                label="Email"
                name="email"
                value={userForm.email}
                onChange={handleUserFormChange}
                fullWidth
                required
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                label="First Name"
                name="firstName"
                value={userForm.firstName}
                onChange={handleUserFormChange}
                fullWidth
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                label="Last Name"
                name="lastName"
                value={userForm.lastName}
                onChange={handleUserFormChange}
                fullWidth
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Department"
                name="department"
                value={userForm.department ?? ''}
                onChange={handleUserFormChange}
                fullWidth
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Password"
                name="password"
                type="password"
                value={userForm.password}
                onChange={handleUserFormChange}
                fullWidth
                required
              />
            </Grid>
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel id="create-role-label">Role</InputLabel>
                <Select
                  labelId="create-role-label"
                  label="Role"
                  name="roleId"
                  value={userForm.roleId || ''}
                  onChange={handleUserFormChange}
                >
                  {roles.map((role) => (
                    <MenuItem key={role.id} value={role.id}>
                      {role.displayName ?? role.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseCreateDialog} color="inherit">
            Cancel
          </Button>
          <Button
            onClick={handleCreateUser}
            variant="contained"
            color="primary"
            disabled={savingUser}
          >
            {savingUser ? 'Creating...' : 'Create'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog open={editDialogOpen} onClose={handleCloseEditDialog} fullWidth maxWidth="sm">
        <DialogTitle>Edit User</DialogTitle>
        <DialogContent dividers>
          <Grid container spacing={2}>
            <Grid item xs={12} md={6}>
              <TextField
                label="Username"
                name="username"
                value={userForm.username}
                InputProps={{ readOnly: true }}
                fullWidth
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                label="Email"
                name="email"
                value={userForm.email}
                onChange={handleUserFormChange}
                fullWidth
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                label="First Name"
                name="firstName"
                value={userForm.firstName}
                onChange={handleUserFormChange}
                fullWidth
              />
            </Grid>
            <Grid item xs={12} md={6}>
              <TextField
                label="Last Name"
                name="lastName"
                value={userForm.lastName}
                onChange={handleUserFormChange}
                fullWidth
              />
            </Grid>
            <Grid item xs={12}>
              <TextField
                label="Department"
                name="department"
                value={userForm.department ?? ''}
                onChange={handleUserFormChange}
                fullWidth
              />
            </Grid>
            <Grid item xs={12}>
              <FormControl fullWidth>
                <InputLabel id="edit-role-label">Role</InputLabel>
                <Select
                  labelId="edit-role-label"
                  label="Role"
                  name="roleId"
                  value={userForm.roleId || ''}
                  onChange={handleUserFormChange}
                >
                  {roles.map((role) => (
                    <MenuItem key={role.id} value={role.id}>
                      {role.displayName ?? role.name}
                    </MenuItem>
                  ))}
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={handleCloseEditDialog} color="inherit">
            Cancel
          </Button>
          <Button
            onClick={handleUpdateUser}
            variant="contained"
            color="primary"
            disabled={savingUser}
          >
            {savingUser ? 'Saving...' : 'Save Changes'}
          </Button>
        </DialogActions>
      </Dialog>
    </Container>
  );
};

export default Users;
