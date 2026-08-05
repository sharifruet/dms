import React from 'react';
import { Box, Chip, Tooltip, Typography } from '@mui/material';
import CheckCircleIcon from '@mui/icons-material/CheckCircle';
import RadioButtonUncheckedIcon from '@mui/icons-material/RadioButtonUnchecked';
import BlockIcon from '@mui/icons-material/Block';
import ErrorOutlineIcon from '@mui/icons-material/ErrorOutline';
import PlayCircleFilledIcon from '@mui/icons-material/PlayCircleFilled';
import { StageReadiness } from '../../types/procurement';

interface Props {
  progress: StageReadiness[];
  activeStage: number;
  onSelect: (stageCode: number) => void;
}

/**
 * The 16-stage rail. It is the primary navigation of the workspace, so it has to
 * answer at a glance: where is this package, and what is in the way.
 */
const StageRail: React.FC<Props> = ({ progress, activeStage, onSelect }) => {
  const iconFor = (stage: StageReadiness) => {
    if (!stage.applicable) return <BlockIcon fontSize="small" sx={{ color: 'text.disabled' }} />;
    if (stage.status === 'COMPLETED') return <CheckCircleIcon fontSize="small" color="success" />;
    if (stage.status === 'REWORK') return <ErrorOutlineIcon fontSize="small" color="error" />;
    if (stage.status === 'IN_PROGRESS') return <PlayCircleFilledIcon fontSize="small" color="primary" />;
    return <RadioButtonUncheckedIcon fontSize="small" sx={{ color: 'text.disabled' }} />;
  };

  const summaryFor = (stage: StageReadiness): string => {
    if (!stage.applicable) return 'Not applicable';
    if (stage.status === 'COMPLETED') return 'Complete';
    const reasons = [
      ...stage.blockers,
      ...stage.missingDocuments.map((d) => `Missing: ${d}`),
      ...stage.unconfirmedFields.map((f) => `Unverified: ${f}`),
      ...stage.validationErrors,
    ];
    if (reasons.length === 0) return 'Ready to complete';
    return reasons.join('\n');
  };

  const blockerCount = (stage: StageReadiness) =>
    stage.blockers.length +
    stage.missingDocuments.length +
    stage.unconfirmedFields.length +
    stage.validationErrors.length;

  return (
    <Box sx={{ borderRight: 1, borderColor: 'divider', minWidth: 260, py: 1 }}>
      {progress.map((stage) => {
        const isActive = stage.stageCode === activeStage;
        const blockers = blockerCount(stage);
        return (
          <Tooltip
            key={stage.stageCode}
            title={<span style={{ whiteSpace: 'pre-line' }}>{summaryFor(stage)}</span>}
            placement="right"
          >
            <Box
              onClick={() => onSelect(stage.stageCode)}
              sx={{
                display: 'flex',
                alignItems: 'center',
                gap: 1,
                px: 2,
                py: 1,
                cursor: 'pointer',
                borderLeft: 3,
                borderColor: isActive ? 'primary.main' : 'transparent',
                backgroundColor: isActive ? 'action.selected' : 'transparent',
                '&:hover': { backgroundColor: 'action.hover' },
              }}
            >
              {iconFor(stage)}
              <Box sx={{ flexGrow: 1, minWidth: 0 }}>
                <Typography
                  variant="body2"
                  noWrap
                  sx={{
                    fontWeight: isActive ? 600 : 400,
                    color: stage.applicable ? 'text.primary' : 'text.disabled',
                  }}
                >
                  {stage.stageCode}. {stage.stageName}
                </Typography>
              </Box>
              {stage.status !== 'COMPLETED' && stage.applicable && blockers > 0 && (
                <Chip label={blockers} size="small" color="warning" sx={{ height: 20 }} />
              )}
              {stage.ready && stage.status !== 'COMPLETED' && stage.applicable && (
                <Chip label="ready" size="small" color="success" sx={{ height: 20 }} />
              )}
            </Box>
          </Tooltip>
        );
      })}
    </Box>
  );
};

export default StageRail;
