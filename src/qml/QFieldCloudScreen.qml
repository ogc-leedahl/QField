import QtQuick 2.12
import QtQuick.Controls 2.12
import QtQuick.Layouts 1.4

import org.qfield 1.0
import Theme 1.0

Page {
  id: qfieldcloudScreen
  signal finished
  property QFieldCloudConnection connection: QFieldCloudConnection
  {
    url: "http://dev.qfield.cloud"
    onStatusChanged: {
        if ( status == QFieldCloudConnection.LoggedIn ) {
          projects.visible = true
          connectionSettings.visible = false
          usernameField.text = connection.username
        }
    }
    onLoginFailed: displayToast( qsTr( "Login failed" ) )
  }

  QFieldCloudProjectsModel {
    id: projectsModel
    cloudConnection: connection

    onProjectDownloaded: failed ? displayToast( qsTr( "Project %1 failed to download" ).arg( projectName ) ) :
                                  displayToast( qsTr( "Project %1 successfully downloaded, it's now available to open" ).arg( projectName ) );
    onWarning: displayToast( message )
  }

  header: PageHeader {
      title: qsTr("QField Cloud")

      showApplyButton: false
      showCancelButton: true

      onFinished: parent.finished()
    }

  ColumnLayout {
    anchors.fill: parent
    Layout.fillWidth: true
    Layout.fillHeight: true
    spacing: 2

    RowLayout {
        id: connectionInformation
        spacing: 2
        Layout.fillWidth: true
        visible: connection.hasToken || projectsModel.rowCount() > 0

        Label {
            Layout.fillWidth: true
            padding: 10
            opacity: projects.visible ? 1 : 0
            text: switch(connection.status) {
                    case 0: qsTr( 'Disconnected from the cloud.' ); break;
                    case 1: qsTr( 'Connecting to the cloud.' ); break;
                    case 2: qsTr( 'Greetings %1.' ).arg( connection.username ); break;
                  }
            wrapMode: Text.WordWrap
            font: Theme.tipFont
        }

        ToolButton {
          Layout.alignment: Qt.AlignTop

          height: 56
          width: 56
          visible: true

          contentItem: Rectangle {
            anchors.fill: parent
            height: 56
            width: 56
            color: "transparent"
            Image {
              anchors.fill: parent
              fillMode: Image.Pad
              horizontalAlignment: Image.AlignHCenter
              verticalAlignment: Image.AlignVCenter
              source: !projects.visible ? Theme.getThemeIcon( 'ic_clear_black_18dp' ) : Theme.getThemeIcon( 'ic_gear_black_24dp' )
            }
          }

          onClicked: {
              if (!connectionSettings.visible) {
                connectionSettings.visible = true
                projects.visible = false
                usernameField.forceActiveFocus()
              } else {
                connectionSettings.visible = false
                projects.visible = true
                refreshProjectsListBtn.forceActiveFocus()
              }
          }
       }
    }

    ColumnLayout {
      id: connectionSettings
      Layout.fillWidth: true
      Layout.fillHeight: true
      Layout.margins: 10
      Layout.topMargin: !connectionInformation.visible ? connectionInformation.height + parent.spacing : 0
      spacing: 2

      Text {
          id: cloudDescriptionLabel
          Layout.alignment: Qt.AlignLeft
          Layout.fillWidth: true
          text: qsTr( "Please file required details to connect to your account." )
          font: Theme.defaultFont
          wrapMode: Text.WordWrap
      }

      Text {
          id: usernamelabel
          Layout.alignment: Qt.AlignHCenter
          Layout.topMargin: 20
          text: qsTr( "Username" )
          font: Theme.defaultFont
          color: 'gray'
      }

      TextField {
          id: usernameField
          Layout.alignment: Qt.AlignHCenter
          Layout.preferredWidth: Math.max( parent.width / 2, usernamelabel.width )
          enabled: connection.status === QFieldCloudConnection.Disconnected
          height: fontMetrics.height + 20
          font: Theme.defaultFont

          background: Rectangle {
              y: usernameField.height - height * 2 - usernameField.bottomPadding / 2
              implicitWidth: parent.width
              height: usernameField.activeFocus ? 2 : 1
              color: usernameField.activeFocus ? "#4CAF50" : "#C8E6C9"
          }
      }

      Text {
          id: passwordlabel
          Layout.alignment: Qt.AlignHCenter
          text: qsTr( "Password" )
          font: Theme.defaultFont
          color: 'gray'
      }

      TextField {
          id: passwordField
          echoMode: TextInput.Password
          Layout.alignment: Qt.AlignHCenter
          Layout.preferredWidth: Math.max( parent.width / 2, usernamelabel.width )
          enabled: connection.status === QFieldCloudConnection.Disconnected
          height: fontMetrics.height + 20
          font: Theme.defaultFont

          background: Rectangle {
              y: passwordField.height - height * 2 - passwordField.bottomPadding / 2
              implicitWidth: parent.width
              height: passwordField.activeFocus ? 2 : 1
              color: passwordField.activeFocus ? "#4CAF50" : "#C8E6C9"
          }

          Keys.onReturnPressed: loginFormSumbitHandler()
      }

      FontMetrics {
        id: fontMetrics
        font: usernameField.font
      }

      QfButton {
          Layout.fillWidth: true
          Layout.topMargin: 5
          text: connection.status == QFieldCloudConnection.LoggedIn ? qsTr( "Logout" ) : connection.status == QFieldCloudConnection.Connecting ? qsTr( "Logging in, please wait" ) : qsTr( "Login" )
          enabled: connection.status != QFieldCloudConnection.Connecting

          onClicked: loginFormSumbitHandler()
      }

      Item {
          Layout.fillHeight: true
          height: 15
      }
    }

    ColumnLayout {
      id: projects
      Layout.fillWidth: true
      Layout.fillHeight: true
      Layout.margins: 10
      Layout.topMargin: 0
      spacing: 2

      Rectangle {
          Layout.fillWidth: true
          Layout.fillHeight: true
          color: "white"
          border.color: "lightgray"
          border.width: 1

          ListView {
              id: table
              anchors.fill: parent

              model: projectsModel
              clip: true

              delegate: Rectangle {
                  id: rectangle
                  property string projectId: Id
                  property string projectOwner: Owner
                  property string projectName: Name
                  property string projectLocalPath: LocalPath
                  width: parent.width
                  height: line.height
                  color: "transparent"

                  ProgressBar {
                      anchors.left: line.left
                      anchors.leftMargin: 4
                      anchors.verticalCenter: line.verticalCenter
                      width: type.width - 4
                      height: 6
                      value: DownloadProgress
                      visible: Status === QFieldCloudProjectsModel.ProjectStatus.Downloading
                      z: 1
                  }

                  Row {
                      id: line
                      Layout.fillWidth: true
                      leftPadding: 6
                      rightPadding: 10
                      topPadding: 9
                      bottomPadding: 3
                      spacing: 0

                      Image {
                          id: type
                          anchors.verticalCenter: inner.verticalCenter
                          source: {
                            if ( connection.status !== QFieldCloudConnection.LoggedIn ) {
                              return Theme.getThemeIcon('ic_cloud_project_offline_48dp')
                            } else {
                              var status = ''

                              switch (Status) {
                                case QFieldCloudProjectsModel.ProjectStatus.Downloading:
                                  return Theme.getThemeIcon('ic_cloud_project_download_48dp')
                                case QFieldCloudProjectsModel.ProjectStatus.Uploading:
                                  return Theme.getThemeIcon('ic_cloud_project_download_48dp')
                                default:
                                  break
                              }

                              switch (Checkout) {
                                case QFieldCloudProjectsModel.LocalCheckout:
                                  return Theme.getThemeIcon('ic_cloud_project_localonly_48dp')
                                default:
                                  break
                              }

                              return Theme.getThemeIcon('ic_cloud_project_48dp')
                            }
                          }
                          sourceSize.width: 80
                          sourceSize.height: 80
                          width: 40
                          height: 40
                          opacity: Status === QFieldCloudProjectsModel.ProjectStatus.Downloading ? 0.3 : 1
                      }
                      ColumnLayout {
                          id: inner
                          width: rectangle.width - type.width - 10
                          Text {
                              id: projectTitle
                              topPadding: 5
                              leftPadding: 3
                              text: Name
                              font.pointSize: Theme.tipFont.pointSize
                              font.underline: true
                              color: Theme.mainColor
                              wrapMode: Text.WordWrap
                              Layout.fillWidth: true
                          }
                          Text {
                              id: projectNote
                              leftPadding: 3
                              text: {
                                if ( connection.status !== QFieldCloudConnection.LoggedIn ) {
                                  return qsTr( '(Available locally)' )
                                } else {
                                  var status = ''

                                  // TODO I think these should be shown as UI badges
                                  switch (Status) {
                                    case QFieldCloudProjectsModel.ProjectStatus.Idle:
                                      break
                                    case QFieldCloudProjectsModel.ProjectStatus.Downloading:
                                      status = qsTr( 'Downloading…' )
                                      break
                                    case QFieldCloudProjectsModel.ProjectStatus.Uploading:
                                      status = qsTr( 'Uploading…' )
                                      break
                                    case QFieldCloudProjectsModel.ProjectStatus.Error:
                                      status = qsTr( 'Error!' )
                                      break
                                    default:
                                      break
                                  }

                                  if ( ! status ) {
                                    switch (Checkout) {
                                      case QFieldCloudProjectsModel.LocalCheckout:
                                        status = qsTr( 'Available locally, missing on the cloud' )
                                        break
                                      case QFieldCloudProjectsModel.RemoteCheckout:
                                        status = qsTr( 'Available on the cloud, missing locally' )
                                        break
                                      case QFieldCloudProjectsModel.LocalFromRemoteCheckout:
                                        status = qsTr( 'Available locally' )
                                        break
                                      default:
                                        break
                                    }
                                  }

                                  return '%1 (%2)'.arg(Description).arg(status)
                                }
                              }
                              visible: text != ""
                              font.pointSize: Theme.tipFont.pointSize - 2
                              font.italic: true
                              wrapMode: Text.WordWrap
                              Layout.fillWidth: true
                          }
                      }
                  }
              }

              MouseArea {
                property Item pressedItem
                anchors.fill: parent
                onClicked: {
                  var item = table.itemAt(mouse.x, mouse.y)
                  if (item) {
                    if (item.projectLocalPath != '') {
                      qfieldcloudScreen.visible = false
                      iface.loadProject(item.projectLocalPath);
                    } else {
                      // fetch remote project
                      displayToast( qsTr( "Downloading project %1" ).arg( item.projectName ) )
                      projectsModel.downloadProject( item.projectId )
                    }
                  }
                }
                onPressed: {
                  var item = table.itemAt(mouse.x, mouse.y)
                  if (item) {
                    pressedItem = item.children[1].children[1].children[0];
                    pressedItem.color = "#5a8725"
                  }
                }
                onCanceled: {
                  if (pressedItem) {
                    pressedItem.color = Theme.mainColor
                    pressedItem = null
                  }
                }
                onReleased: {
                  if (pressedItem) {
                    pressedItem.color = Theme.mainColor
                    pressedItem = null
                  }
                }

                onPressAndHold: {
                    var item = table.itemAt(mouse.x, mouse.y)
                    if (item) {
                      projectActions.projectId = item.projectId
                      projectActions.projectOwner = item.projectOwner
                      projectActions.projectName = item.projectName
                      projectActions.projectLocalPath = item.projectLocalPath
                      downloadProject.visible = item.projectLocalPath == ''
                      openProject.visible = item.projectLocalPath !== ''
                      removeProject.visible = item.projectLocalPath !== ''
                      projectActions.popup(mouse.x, mouse.y)
                    }
                }
              }
          }
      }

      Menu {
        id: projectActions

        property string projectId: ''
        property string projectOwner: ''
        property string projectName: ''
        property string projectLocalPath: ''

        title: 'Project Actions'
        width: Math.max(320, mainWindow.width/2)

        MenuItem {
          id: downloadProject

          font: Theme.defaultFont
          width: parent.width
          height: visible ? 48 : 0
          leftPadding: 10

          text: qsTr( "Download Project" )
          onTriggered: {
            projectsModel.downloadProject(projectActions.projectId)
          }
        }

        MenuItem {
          id: openProject

          font: Theme.defaultFont
          width: parent.width
          height: visible ? 48 : 0
          leftPadding: 10

          text: qsTr( "Open Project" )
          onTriggered: {
            if ( projectActions.projectLocalPath != '') {
              qfieldcloudScreen.visible = false
              iface.loadProject(projectActions.projectLocalPath);
            }
          }
        }

        MenuItem {
          id: removeProject

          font: Theme.defaultFont
          width: parent.width
          height: visible ? 48 : 0
          leftPadding: 10

          text: qsTr( "Remove Stored Project" )
          onTriggered: {
            projectsModel.removeLocalProject(projectActions.projectId)
          }
        }
      }

      Text {
          id: projectsTips
          Layout.alignment: Qt.AlignLeft
          Layout.fillWidth: true
          Layout.topMargin: 5
          Layout.bottomMargin: 5
          text: qsTr( "Press and hold over a cloud project for a menu of additional actions." )
          font: Theme.tipFont
          wrapMode: Text.WordWrap
      }

      QfButton {
          id: refreshProjectsListBtn
          Layout.fillWidth: true
          text: qsTr( "Refresh projects list" )
          enabled: connection.status == QFieldCloudConnection.LoggedIn

          onClicked: projectsModel.refreshProjectsList()
      }
    }
  }

  function prepareCloudLogin() {
    if ( visible ) {
      usernameField.text = connection.username
      if ( connection.status == QFieldCloudConnection.Disconnected ) {
        if ( connection.hasToken ) {
          connection.login();

          projects.visible = true
          connectionSettings.visible = false
        } else {
          projects.visible = false
          connectionSettings.visible = true
        }
      } else {
        projects.visible = true
        connectionSettings.visible = false
      }
    }
  }

  function loginFormSumbitHandler() {
    if (connection.status == QFieldCloudConnection.LoggedIn) {
      connection.logout()
    } else {
      connection.username = usernameField.text
      connection.password = passwordField.text
      connection.login()
    }
  }

  Component.onCompleted: {
    prepareCloudLogin()
  }

  onVisibleChanged: {
    prepareCloudLogin()
  }

  Keys.onReleased: {
    if (event.key === Qt.Key_Back || event.key === Qt.Key_Escape) {
      event.accepted = true
      finished()
    }
  }
}