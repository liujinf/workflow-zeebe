import React from 'react';
import moment from 'moment';
import {Link, Redirect} from 'react-router-dom';

import {Button, Input} from 'components';
import {loadDashboard, remove, update} from './service';
import EditGrid from './ReportGrid/EditGrid';

import './Dashboard.css';

export default class Dashboard extends React.Component {
  constructor(props) {
    super(props);

    this.id = props.match.params.id;

    this.state = {
      name: null,
      lastModified: null,
      lastModifier: null,
      loaded: false,
      redirect: false,
      originalName: null,
      reports: [],
      originalReports : []
    };

    this.load();
  }

  load = async () => {
    const {name, lastModifier, lastModified, reports} = await loadDashboard(this.id);
    this.setState({
      name,
      lastModifier,
      lastModified,
      loaded: true,
      originalName: name,
      reports: reports ? reports : [],
      originalReports: reports ? JSON.parse(JSON.stringify(reports)) : []
    });
  }

  deleteDashboard = async evt => {
    await remove(this.id);

    this.setState({
      redirect: true
    });
  }

  updateName = evt => {
    this.setState({
      name : evt.target.value
    });
  }

  saveChanges = async () => {
    await update(this.id, {
      name : this.state.name,
      reports : this.state.reports
    });

    this.setState({
      originalName: this.state.name,
      originalReports: JSON.parse(JSON.stringify(this.state.reports))
    });
  }

  cancelChanges = async () => {
    this.setState({
      name : this.state.originalName,
      reports: JSON.parse(JSON.stringify(this.state.originalReports))
    });
  }

  handleReportSelection = (report, position, dimensions) => {
    let reports = JSON.parse(JSON.stringify(this.state.reports));
    reports.push({
      id : report.id,
      dimensions: dimensions,
      position: position,
      name: report.name
    });
    this.setState({reports: reports});
  }

  handleReportRemoval = (report) => {
    console.log('removing report [' + report.id + '] from dashboard')
  }

  handleReportMove = (oldReport, newReport) => {
    let reports = JSON.parse(JSON.stringify(this.state.reports));

    for (let i = 0; i < reports.length; i++) {
      if (reports[i].position.x === oldReport.position.x && reports[i].position.y === oldReport.position.y) {
        reports[i] = newReport;
      }
    }
    this.setState({reports: reports});
  }

  renderEditMode = (state) => {
    const {name, lastModifier, lastModified} = state;

    return (
      <div className='Dashboard'>
        <div className='Dashboard__header'>
          <div className='Dashboard__name-container'>
            <Input type="text" id={'name'} onChange={this.updateName} value={name || ''} className='Dashboard__name-input'></Input>
            <div className='Dashboard__metadata'>Last modified {moment(lastModified).format('lll')} by {lastModifier}</div>
          </div>
          <div className='Dashboard__tools'>
            <Link id={'save'} className='Button Dashboard__tool-button' to={`/dashboard/${this.id}`} onClick={this.saveChanges.bind(this)}>Save</Link>
            <Link id={'cancel'} className='Button Dashboard__tool-button' to={`/dashboard/${this.id}`} onClick={this.cancelChanges}>Cancel</Link>
          </div>
        </div>
        <EditGrid
            reports={state.reports}
            onReportSelected={this.handleReportSelection.bind(this)}
            onReportRemoved={this.handleReportRemoval.bind(this)}
            onReportMoved={this.handleReportMove.bind(this)}
        />
      </div>
    )
  }


  renderViewMode = (state) => {
    const {name, lastModifier, lastModified} = state;

    return (
      <div className='Dashboard'>
        <div className='Dashboard__header'>
          <div className='Dashboard__name-container'>
            <h1 className='Dashboard__heading'>{name}</h1>
            <div className='Dashboard__metadata'>Last modified {moment(lastModified).format('lll')} by {lastModifier}</div>
          </div>
          <div className='Dashboard__tools'>
            <Link id={'edit'} className='Button Dashboard__tool-button' to={`/dashboard/${this.id}/edit`}>Edit</Link>
            <Button onClick={this.deleteDashboard} className='Dashboard__tool-button'>Delete</Button>
          </div>
        </div>
      </div>
    )
  }

  render() {
    const {viewMode} = this.props.match.params;

    const {loaded, redirect} = this.state;


    if(!loaded) {
      return <div className = 'dashboard-loading-indicator'>loading...</div>;
    }

    if(redirect) {
      return <Redirect to='/dashboards' />;
    }

    return (<div>
      {viewMode === 'edit' ? (this.renderEditMode(this.state)) : (this.renderViewMode(this.state))}
    </div>);
  }
}
