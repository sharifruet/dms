import api from './api';

export const contractAgreementService = {
  getTotalCount: async (): Promise<number> => {
    const response = await api.get<number>('/contract-agreements/count');
    return response.data ?? 0;
  },

  getRunningCount: async (): Promise<number> => {
    const response = await api.get<number>('/contract-agreements/running/count');
    return response.data ?? 0;
  },
};

export default contractAgreementService;
