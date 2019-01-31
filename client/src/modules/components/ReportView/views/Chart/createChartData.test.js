import createChartData from './createChartData';

import {uniteResults} from '../service';

jest.mock('../service', () => {
  return {
    uniteResults: jest.fn().mockReturnValue([{foo: 123, bar: 5}])
  };
});

jest.mock('./colorsUtils', () => {
  const rest = jest.requireActual('./colorsUtils');
  return {
    ...rest,
    createColors: jest.fn().mockReturnValue([])
  };
});

it('should create two datasets for line chart with target values', () => {
  const result = {foo: 123, bar: 5, dar: 5};
  const targetValue = {target: 10};

  const chartData = createChartData({
    result,
    data: {
      configuration: {color: ['blue']},
      visualization: 'line',
      groupBy: {
        type: '',
        value: ''
      }
    },
    combined: false,
    theme: 'light',
    targetValue
  });
  expect(chartData.datasets).toHaveLength(2);
});

it('should create two datasets for each report in combined line charts with target values', () => {
  uniteResults.mockClear();

  const result = {foo: 123, bar: 5, dar: 5};
  uniteResults.mockReturnValue([result, result]);
  const targetValue = {target: 10};
  const chartData = createChartData({
    result: [result, result],
    reportsNames: ['test1', 'test2'],
    data: {
      reportIds: ['reportA', 'reportB'],
      configuration: {reportColors: ['blue', 'yellow']},
      visualization: 'line',
      groupBy: {
        type: '',
        value: ''
      }
    },
    targetValue,
    combined: true,
    theme: 'light'
  });

  expect(chartData.datasets).toHaveLength(4);
});

it('should return correct chart data object for a single report', () => {
  uniteResults.mockClear();
  const result = {foo: 123, bar: 5};
  uniteResults.mockReturnValue([result]);

  const chartData = createChartData({
    result,
    data: {
      configuration: {color: 'testColor'},
      visualization: 'line',
      groupBy: {
        type: '',
        value: ''
      }
    },
    targetValue: false,
    combined: false,
    theme: 'light'
  });

  expect(chartData).toEqual({
    labels: ['foo', 'bar'],
    datasets: [
      {
        legendColor: 'testColor',
        data: [123, 5],
        borderColor: 'testColor',
        backgroundColor: 'transparent',
        borderWidth: 2
      }
    ]
  });
});

it('should return correct chart data object for a combined report', () => {
  const result = [{foo: 123, bar: 5}, {foo: 1, dar: 3}];

  uniteResults.mockClear();
  uniteResults.mockReturnValue(result);

  const chartData = createChartData({
    reportsNames: ['Report A', 'Report B'],
    result,
    data: {
      groupBy: {
        type: '',
        value: ''
      },
      reportIds: ['reportA', 'reportB'],
      configuration: {reportColors: ['blue', 'yellow']},
      visualization: 'line'
    },
    targetValue: false,
    combined: true,
    theme: 'light'
  });

  expect(chartData).toEqual({
    datasets: [
      {
        backgroundColor: 'transparent',
        borderColor: 'blue',
        borderWidth: 2,
        data: [123, 5],
        label: 'Report A',
        legendColor: 'blue'
      },
      {
        backgroundColor: 'transparent',
        borderColor: 'yellow',
        borderWidth: 2,
        data: [1, 3],
        label: 'Report B',
        legendColor: 'yellow'
      }
    ],
    labels: ['foo', 'bar', 'dar']
  });
});
