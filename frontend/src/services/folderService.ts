import api from './api';

export interface Folder {
  id: number;
  name: string;
  description?: string;
  parentFolder?: Folder | null;
  subFolders?: Folder[];
  folderPath: string;
  department?: string;
  createdBy: {
    id: number;
    username: string;
    firstName?: string;
    lastName?: string;
  };
  isActive: boolean;
  isSystemFolder: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface FolderSummary {
  folderId: number;
  folderName: string;
  totalFiles: number;
  uploadedFiles: number;
  remainingUploads: number;
}

export interface CreateFolderRequest {
  name: string;
  description?: string;
  parentFolderId?: number | null;
  department?: string;
}

export interface UpdateFolderRequest {
  name: string;
  description?: string;
  department?: string;
}

export interface MoveFolderRequest {
  newParentFolderId?: number | null;
}

export const folderService = {
  /**
   * Get root folders
   */
  getRootFolders: async (): Promise<Folder[]> => {
    const response = await api.get('/folders/root');
    return response.data.folders || [];
  },

  /**
   * Get folder tree (hierarchy)
   */
  getFolderTree: async (): Promise<Folder[]> => {
    const response = await api.get('/folders/tree');
    return response.data.tree || [];
  },

  /**
   * Get folder by ID
   */
  getFolder: async (id: number): Promise<Folder> => {
    const response = await api.get(`/folders/${id}`);
    return response.data.folder;
  },

  /**
   * Get subfolders of a folder
   */
  getSubFolders: async (id: number): Promise<Folder[]> => {
    const response = await api.get(`/folders/${id}/subfolders`);
    return response.data.subFolders || [];
  },

  /**
   * Get folder summary
   */
  getFolderSummary: async (id: number): Promise<FolderSummary> => {
    const response = await api.get(`/folders/${id}/summary`);
    return response.data.summary;
  },

  /**
   * Create a new folder
   */
  createFolder: async (request: CreateFolderRequest): Promise<Folder> => {
    const response = await api.post('/folders', request);
    return response.data.folder;
  },

  /**
   * Update folder
   */
  updateFolder: async (id: number, request: UpdateFolderRequest): Promise<Folder> => {
    const response = await api.put(`/folders/${id}`, request);
    return response.data.folder;
  },

  /**
   * Delete folder
   */
  deleteFolder: async (id: number): Promise<void> => {
    await api.delete(`/folders/${id}`);
  },

  /**
   * Move folder to new parent
   */
  moveFolder: async (id: number, request: MoveFolderRequest): Promise<Folder> => {
    const response = await api.post(`/folders/${id}/move`, request);
    return response.data.folder;
  },
};

