import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Add request interceptor to include JWT token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Add response interceptor to handle token expiration
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token');
      localStorage.removeItem('user');
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export interface MLModel {
  id?: number;
  modelName: string;
  modelType: string;
  modelDescription?: string;
  modelVersion?: string;
  trainingData?: string;
  modelParameters?: string;
  performanceMetrics?: string;
  status: string;
  accuracy?: number;
  precisionScore?: number;
  recallScore?: number;
  f1Score?: number;
  trainingStartedAt?: string;
  trainingCompletedAt?: string;
  lastTrainedAt?: string;
  deploymentStatus: string;
  deployedAt?: string;
  modelSizeMb?: number;
  isActive: boolean;
  createdAt?: string;
  updatedAt?: string;
  createdBy?: string;
  updatedBy?: string;
}

export interface MLModelPerformance {
  accuracy: number;
  precision: number;
  recall: number;
  f1Score: number;
  confusionMatrix: number[][];
  rocCurve: { x: number; y: number }[];
  trainingHistory: { epoch: number; loss: number; accuracy: number }[];
}

export interface MLModelPrediction {
  prediction: any;
  confidence: number;
  probabilities: { [key: string]: number };
  processingTime: number;
  modelVersion: string;
}

export interface MLModelStatistics {
  totalModels: number;
  activeModels: number;
  trainingModels: number;
  deployedModels: number;
  averageAccuracy: number;
  totalPredictions: number;
}

export interface MLModelLog {
  id: number;
  modelId: number;
  logLevel: string;
  message: string;
  details?: string;
  timestamp: string;
}

export interface MLInsights {
  topPerformingModels: MLModel[];
  recentTrainingResults: MLModel[];
  predictionTrends: { date: string; count: number }[];
  accuracyTrends: { date: string; accuracy: number }[];
}

export interface MLRecommendation {
  id: string;
  type: string;
  title: string;
  description: string;
  priority: string;
  actionRequired: boolean;
  estimatedImpact: string;
}

export const mlService = {
  // Create a new ML model
  createMLModel: async (model: MLModel): Promise<MLModel> => {
    const response = await api.post('/ml/models', model);
    return response.data;
  },

  // Get all ML models
  getMLModels: async (params?: {
    modelType?: string;
    status?: string;
    version?: string;
    page?: number;
    size?: number;
  }): Promise<{ content: MLModel[]; totalElements: number; totalPages: number }> => {
    const response = await api.get('/ml/models', { params });
    return response.data;
  },

  // Get ML model by ID
  getMLModelById: async (id: number): Promise<MLModel> => {
    const response = await api.get(`/ml/models/${id}`);
    return response.data;
  },

  // Update ML model
  updateMLModel: async (id: number, model: MLModel): Promise<MLModel> => {
    const response = await api.put(`/ml/models/${id}`, model);
    return response.data;
  },

  // Delete ML model
  deleteMLModel: async (id: number): Promise<void> => {
    await api.delete(`/ml/models/${id}`);
  },

  // Train ML model
  trainMLModel: async (id: number): Promise<{ success: boolean; message: string; trainingId?: string }> => {
    const response = await api.post(`/ml/models/${id}/train`);
    return response.data;
  },

  // Deploy ML model
  deployMLModel: async (id: number): Promise<{ success: boolean; message: string; deploymentId?: string }> => {
    const response = await api.post(`/ml/models/${id}/deploy`);
    return response.data;
  },

  // Get ML model predictions
  getMLModelPredictions: async (id: number, inputData: any): Promise<MLModelPrediction> => {
    const response = await api.post(`/ml/models/${id}/predict`, inputData);
    return response.data;
  },

  // Get ML model performance
  getMLModelPerformance: async (id: number): Promise<MLModelPerformance> => {
    const response = await api.get(`/ml/models/${id}/performance`);
    return response.data;
  },

  // Get ML model statistics
  getMLModelStatistics: async (): Promise<MLModelStatistics> => {
    const response = await api.get('/ml/statistics');
    return response.data;
  },

  // Get ML model logs
  getMLModelLogs: async (id: number): Promise<MLModelLog[]> => {
    const response = await api.get(`/ml/models/${id}/logs`);
    return response.data;
  },

  // Get ML insights
  getMLInsights: async (): Promise<MLInsights> => {
    const response = await api.get('/ml/insights');
    return response.data;
  },

  // Get ML recommendations
  getMLRecommendations: async (params?: {
    userId?: string;
    context?: string;
  }): Promise<MLRecommendation[]> => {
    const response = await api.get('/ml/recommendations', { params });
    return response.data;
  },

  // Get model training status
  getTrainingStatus: async (id: number): Promise<{
    status: string;
    progress: number;
    currentEpoch: number;
    totalEpochs: number;
    currentLoss: number;
    estimatedTimeRemaining: number;
  }> => {
    const response = await api.get(`/ml/models/${id}/training-status`);
    return response.data;
  },

  // Stop model training
  stopTraining: async (id: number): Promise<{ success: boolean; message: string }> => {
    const response = await api.post(`/ml/models/${id}/stop-training`);
    return response.data;
  }
};

export default mlService;
