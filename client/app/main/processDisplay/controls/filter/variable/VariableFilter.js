import {jsx, withSelector, Scope, Text, OnEvent} from 'view-utils';
import operatorLabels from './labels';

export const VariableFilter = withSelector(({onDelete}) => {
  return <span>
    <button type="button" className="btn btn-link btn-xs pull-right">
      <OnEvent event="click" listener={onDelete} />
      ×
    </button>
    <span>
      <span className="variable-name">
        <Text property="name" />
      </span>
      &nbsp;
      <span className="variable-operator">
        <Scope selector={getHumanReadableLabelForOperator}>
          <Text property="operator" />
        </Scope>
      </span>
      &nbsp;
      <span className="variable-value badge">
        <Scope selector={getFirstValue}>
          <Text property="value" />
        </Scope>
      </span>
    </span>
  </span>;

  function getHumanReadableLabelForOperator({operator}) {
    return {operator: operatorLabels[operator]};
  }

  function getFirstValue({values}) {
    return {value: values[0]};
  }
});
