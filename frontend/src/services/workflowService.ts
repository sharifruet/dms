import api from './api';

export interface Workflow {
  id: number;
  name: string;
  description: string;
  type: string;
  status: string;
  createdBy: {
    id: number;
    username: string;
  };
  definition: string;
  triggerConditions: string;
  isAutomatic: boolean;
  isPublic: boolean;
  executionCount: number;
  lastExecutedAt: string;
  nextExecutionAt: string;
  createdAt: string;
  updatedAt: string;
}

export interface WorkflowInstance {
  id: number;
  workflow: Workflow;
  document: {
    id: number;
    originalName: string;
  };
  initiatedBy: {
    id: number;
    username: string;
  };
  status: string;
  currentStep: number;
  totalSteps: number;
  contextData: string;
  startedAt: string;
  completedAt: string;
  dueDate: string;
  priority: number;
  notes: string;
  createdAt: string;
  updatedAt: string;
}

export interface WorkflowStep {
  id: number;
  workflowInstance: WorkflowInstance;
  stepNumber: number;
  stepName: string;
  stepDescription: string;
  stepType: string;
  assignedTo: {
    id: number;
    username: string;
  };
  status: string;
  dueDate: string;
  startedAt: string;
  completedAt: string;
  actionTaken: string;
  comments: string;
  stepData: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateWorkflowRequest {
  name: string;
  description: string;
  type: string;
  definition: string;
}

export interface StartWorkflowRequest {
  documentId: number;
}

export interface CompleteStepRequest {
  actionTaken: string;
  comments: string;
}

export interface RejectStepRequest {
  reason: string;
}

export const workflowService = {
  // Create a new workflow
  createWorkflow: async (request: CreateWorkflowRequest): Promise<Workflow> => {
    const response = await api.post('/workflows', request);
    return response.data;
  },

  // Start a workflow instance
  startWorkflow: async (workflowId: number, request: StartWorkflowRequest): Promise<WorkflowInstance> => {
    const response = await api.post(`/workflows/${workflowId}/start`, request);
    return response.data;
  },

  // Complete a workflow step
  completeWorkflowStep: async (stepId: number, request: CompleteStepRequest): Promise<void> => {
    await api.post(`/workflows/steps/${stepId}/complete`, request);
  },

  // Reject a workflow step
  rejectWorkflowStep: async (stepId: number, request: RejectStepRequest): Promise<void> => {
    await api.post(`/workflows/steps/${stepId}/reject`, request);
  },

  // Get workflow instances for user
  getWorkflowInstances: async (page: number = 0, size: number = 10): Promise<{
    content: WorkflowInstance[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  }> => {
    const response = await api.get(`/workflows/instances?page=${page}&size=${size}`);
    return response.data;
  },

  // Get workflow steps assigned to user
  getWorkflowSteps: async (page: number = 0, size: number = 10): Promise<{
    content: WorkflowStep[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  }> => {
    const response = await api.get(`/workflows/steps?page=${page}&size=${size}`);
    return response.data;
  },

  // Get overdue workflow instances
  getOverdueWorkflowInstances: async (): Promise<WorkflowInstance[]> => {
    const response = await api.get('/workflows/overdue');
    return response.data;
  },

  // Get workflow statistics
  getWorkflowStatistics: async (): Promise<{
    totalWorkflows: number;
    activeWorkflows: number;
    totalInstances: number;
    pendingInstances: number;
    inProgressInstances: number;
    completedInstances: number;
    rejectedInstances: number;
  }> => {
    const response = await api.get('/workflows/statistics');
    return response.data;
  },

  // Get active tender workflow instances (for document upload)
  getActiveTenderWorkflowInstances: async (): Promise<WorkflowInstance[]> => {
    const response = await api.get('/workflows/instances/tender/active');
    return response.data;
  }
};
