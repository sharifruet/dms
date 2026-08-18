import React from 'react';
import { render, screen } from '@testing-library/react';
import { Provider } from 'react-redux';
import { configureStore } from '@reduxjs/toolkit';
import authReducer from '../../../store/slices/authSlice';
import FieldRow from '../FieldRow';
import ManualFieldForm from '../ManualFieldForm';
import { CatalogueField, ExtractedField } from '../../../types/procurement';

/**
 * The screen-level rules that carry a decision.
 *
 * <p>Not coverage for its own sake: these are the three places where the UI is the only
 * thing standing between a user and a wrong impression — a field OCR disagrees with, a
 * mandatory value with no way in, and an action the server will refuse. Each was built in
 * response to a real defect, and none had any test behind it.
 */

const store = () => configureStore({
  reducer: { auth: authReducer },
});

const withStore = (ui: React.ReactElement) =>
  render(<Provider store={store()}>{ui}</Provider>);

const field = (overrides: Partial<ExtractedField> = {}): ExtractedField => ({
  id: 1,
  entityType: 'PACKAGE',
  packageId: 1,
  stageCode: 1,
  fieldKey: 'package_number',
  fieldLabel: 'Package Number',
  dataType: 'TEXT',
  textValue: 'GRL-18',
  captureSource: 'MANUAL',
  status: 'VERIFIED',
  ...overrides,
});

describe('a field OCR disagrees with', () => {
  it('shows the confirmed value, the competing reading, and calls it a disagreement', () => {
    // The confirmed value must stand and the conflict must be visible - an OCR pass that
    // read something else silently winning is the defect this came from
    withStore(
      <FieldRow
        field={field({
          validationState: 'CONFLICT',
          validationMessage:
            "OCR now reads 'GRL-l8' but this field was confirmed as 'GRL-18'. "
            + 'The confirmed value has been kept - check the document.',
        })}
        onVerify={jest.fn()}
        onOverride={jest.fn()}
      />,
    );

    expect(screen.getByText(/Disagreement:/)).toBeInTheDocument();
    expect(screen.getByText(/GRL-l8/)).toBeInTheDocument();
    expect(screen.getByText(/confirmed as 'GRL-18'/)).toBeInTheDocument();
  });

  it('does not label an ordinary low-confidence caution as a disagreement', () => {
    withStore(
      <FieldRow
        field={field({
          status: 'OCR_SUGGESTED',
          validationState: 'NEEDS_REVIEW',
          validationMessage: 'Low OCR confidence - please check against the document',
        })}
        onVerify={jest.fn()}
        onOverride={jest.fn()}
      />,
    );

    expect(screen.queryByText(/Disagreement:/)).not.toBeInTheDocument();
    expect(screen.getByText(/Low OCR confidence/)).toBeInTheDocument();
  });
});

describe('fields that have to be typed in', () => {
  const catalogue = (overrides: Partial<CatalogueField> = {}): CatalogueField => ({
    id: 1,
    documentType: 'BER',
    fieldKey: 'oce_value',
    fieldLabel: 'OCE Value (manual entry)',
    fieldType: 'CURRENCY',
    captureSource: 'MANUAL',
    isMandatory: true,
    ...overrides,
  });

  it('offers an input for a manual field that has not been captured', () => {
    // Stages 5, 8 and 9 are manual end to end and the OCE is typed at BER upload. Before
    // this form existed there was no way to enter any of them, so those stages could
    // never be completed.
    withStore(
      <ManualFieldForm catalogue={[catalogue()]} fields={[]} onSave={jest.fn()} />,
    );

    expect(screen.getByLabelText(/OCE Value/)).toBeInTheDocument();
    expect(screen.getByText(/Required before this stage can be completed/)).toBeInTheDocument();
  });

  it('says how many mandatory values are still outstanding', () => {
    withStore(
      <ManualFieldForm
        catalogue={[catalogue(), catalogue({ fieldKey: 'evaluation_date', fieldLabel: 'Evaluation Date' })]}
        fields={[]}
        onSave={jest.fn()}
      />,
    );

    expect(screen.getByText('2 still needed')).toBeInTheDocument();
  });

  it('shows a captured value so the form doubles as a correction', () => {
    withStore(
      <ManualFieldForm
        catalogue={[catalogue()]}
        fields={[field({ fieldKey: 'oce_value', textValue: '4320', dataType: 'CURRENCY' })]}
        onSave={jest.fn()}
      />,
    );

    expect(screen.getByDisplayValue('4320')).toBeInTheDocument();
    expect(screen.queryByText(/still needed/)).not.toBeInTheDocument();
  });

  it('leaves OCR-sourced fields alone - they are verified, not typed', () => {
    const { container } = withStore(
      <ManualFieldForm
        catalogue={[catalogue({ captureSource: 'OCR', fieldKey: 'package_number' })]}
        fields={[]}
        onSave={jest.fn()}
      />,
    );

    // Nothing to render: this form is only for values a person supplies
    expect(container).toBeEmptyDOMElement();
  });
});
