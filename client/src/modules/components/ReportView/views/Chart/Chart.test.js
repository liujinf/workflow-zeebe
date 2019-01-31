import React from 'react';
import {shallow} from 'enzyme';

import ThemedChart from './Chart';
import ChartRenderer from 'chart.js';
const {WrappedComponent: Chart} = ThemedChart;

const props = {
  data: {
    configuration: {targetValue: {active: false}},
    view: {operation: 'count'},
    groupBy: {
      value: '',
      type: ''
    }
  }
};

it('should construct a Chart', () => {
  shallow(<Chart {...props} result={{}} />);

  expect(ChartRenderer).toHaveBeenCalled();
});

it('should display an error message for a non-object result (single number)', () => {
  const node = shallow(<Chart {...props} result={7} errorMessage="Error" />);

  expect(node.find('ReportBlankSlate').prop('message')).toBe('Error');
});

it('should display an error message if no data is provided', () => {
  const node = shallow(<Chart {...props} errorMessage="Error" />);

  expect(node.find('ReportBlankSlate').prop('message')).toBe('Error');
});

it('should use the provided type for the ChartRenderer', () => {
  ChartRenderer.mockClear();

  shallow(
    <Chart
      {...props}
      data={{...props.data, visualization: 'visualization_type'}}
      result={{foo: 123}}
    />
  );

  expect(ChartRenderer.mock.calls[0][1].type).toBe('visualization_type');
});

it('should use the special targetLine type when target values are enabled on a line chart', () => {
  ChartRenderer.mockClear();

  const targetValue = {targetValue: {active: true, countChart: {isBelow: true, value: 1}}};

  shallow(
    <Chart
      {...props}
      result={{foo: 123}}
      data={{...props.data, visualization: 'line', configuration: targetValue}}
    />
  );

  expect(ChartRenderer.mock.calls[0][1].type).toBe('targetLine');
});

it('should change type for the ChartRenderer if props were updated', () => {
  ChartRenderer.mockClear();

  const chart = shallow(
    <Chart
      {...props}
      result={{foo: 123}}
      data={{...props.data, visualization: 'visualization_type'}}
    />
  );

  chart.setProps({data: {...props.data, visualization: 'new_visualization_type'}});

  expect(ChartRenderer.mock.calls[1][1].type).toBe('new_visualization_type');
});

it('should not display an error message if data is valid', () => {
  const node = shallow(<Chart {...props} result={{foo: 123}} errorMessage="Error" />);

  expect(node.find('ReportBlankSlate')).not.toBePresent();
});

it('should destroy chart if no data is provided', () => {
  const node = shallow(<Chart {...props} errorMessage="Error" />);

  expect(node.chart).toBe(undefined);
});

it('should render chart even if type does not change', () => {
  ChartRenderer.mockClear();

  const chart = shallow(
    <Chart
      {...props}
      result={{foo: 123}}
      data={{...props.data, visualization: 'visualization_type'}}
    />
  );
  chart.setProps({data: {...props.data, visualization: 'visualization_type'}});

  expect(ChartRenderer.mock.calls[1][1].type).toBe('visualization_type');
});

it('should display an error message if there was data and the second time no data is provided', () => {
  const node = shallow(<Chart {...props} data={{foo: 123}} errorMessage="Error" />);

  node.setProps({data: null});

  expect(node.find('ReportBlankSlate').prop('message')).toBe('Error');
});
