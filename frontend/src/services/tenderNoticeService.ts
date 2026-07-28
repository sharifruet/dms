import api from './api';

export const tenderNoticeService = {
  getLiveCount: async (): Promise<number> => {
    const response = await api.get<number>('/tender-notices/live/count');
    return response.data ?? 0;
  },
};

export default tenderNoticeService;
