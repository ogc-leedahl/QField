#include "qfieldcloudprojectsmodel.h"
#include "qfieldcloudconnection.h"
#include "qfieldcloudutils.h"

#include <qgis.h>
#include <qgsnetworkaccessmanager.h>
#include <qgsapplication.h>

#include <QNetworkReply>
#include <QTemporaryFile>
#include <QJsonDocument>
#include <QJsonArray>
#include <QJsonObject>
#include <QDir>
#include <QDirIterator>
#include <QDebug>

QFieldCloudProjectsModel::QFieldCloudProjectsModel()
{
  QJsonArray projects;
  reload( projects );
}

QFieldCloudConnection *QFieldCloudProjectsModel::cloudConnection() const
{
  return mCloudConnection;
}

void QFieldCloudProjectsModel::setCloudConnection( QFieldCloudConnection *cloudConnection )
{
  if ( mCloudConnection == cloudConnection )
    return;

  if ( cloudConnection )
    connect( cloudConnection, &QFieldCloudConnection::statusChanged, this, &QFieldCloudProjectsModel::connectionStatusChanged );

  mCloudConnection = cloudConnection;
  emit cloudConnectionChanged();
}

void QFieldCloudProjectsModel::refreshProjectsList()
{
  if ( mCloudConnection->status() == QFieldCloudConnection::Status::LoggedIn )
  {
    QNetworkReply *reply = mCloudConnection->get( QStringLiteral( "/api/v1/projects/%1/" ).arg( mCloudConnection->username() ) );
    connect( reply, &QNetworkReply::finished, this, &QFieldCloudProjectsModel::projectListReceived );
  }
  else if ( mCloudConnection->status() == QFieldCloudConnection::Status::Disconnected )
  {
    QJsonArray projects;
    reload( projects );
  }
}

void QFieldCloudProjectsModel::downloadProject( const QString &owner, const QString &projectName )
{
  if ( !mCloudConnection )
    return;

  for( int i = 0; i < mCloudProjects.count(); i++ )
  {
    if ( mCloudProjects.at( i ).owner == owner && mCloudProjects.at( i ).name == projectName )
    {
      mCloudProjects[i].nbFiles = 0;
      mCloudProjects[i].nbFilesDownloaded = 0;
      mCloudProjects[i].nbFilesFailed = 0;
      mCloudProjects[i].status = Status::Downloading;
      QModelIndex idx = createIndex( i, 0 );
      emit dataChanged( idx, idx,  QVector<int>() << StatusRole );
      break;
    }
  }

  QNetworkReply *filesReply = mCloudConnection->get( QStringLiteral( "/api/v1/projects/%1/%2/files/" ).arg( owner, projectName ) );

  connect( filesReply, &QNetworkReply::finished, this, [filesReply, this, owner, projectName]()
  {
    if ( filesReply->error() == QNetworkReply::NoError )
    {
      QJsonArray files = QJsonDocument::fromJson( filesReply->readAll() ).array();

      for( int i = 0; i < mCloudProjects.count(); i++ )
      {
        if ( mCloudProjects.at( i ).owner == owner && mCloudProjects.at( i ).name == projectName )
        {
          mCloudProjects[i].nbFiles = files.count();
          break;
        }
      }

      for ( const auto file : files )
      {
        downloadFile( owner, projectName, file.toObject().value( QStringLiteral( "name" ) ).toString() );
      }
    }
    else
    {
      for( int i = 0; i < mCloudProjects.count(); i++ )
      {
        if ( mCloudProjects.at( i ).owner == owner && mCloudProjects.at( i ).name == projectName )
        {
          mCloudProjects[i].status = Status::Available;
          break;
        }
      }
      emit warning( QStringLiteral( "Error fetching project: %1" ).arg( filesReply->errorString() ) );
    }

    filesReply->deleteLater();
  } );
}

void QFieldCloudProjectsModel::connectionStatusChanged()
{
  refreshProjectsList();
}

void QFieldCloudProjectsModel::projectListReceived()
{
  QNetworkReply *reply = qobject_cast<QNetworkReply *>( sender() );
  Q_ASSERT( reply );

  if ( reply->error() != QNetworkReply::NoError )
    return;

  QByteArray response = reply->readAll();

  QJsonDocument doc = QJsonDocument::fromJson( response );
  QJsonArray projects = doc.array();
  reload( projects );
}

void QFieldCloudProjectsModel::downloadFile( const QString &owner, const QString &projectName, const QString &fileName )
{
  QNetworkReply *reply = mCloudConnection->get( QStringLiteral( "/api/v1/projects/%1/%2/%3/" ).arg( owner, projectName, fileName ) );

  QTemporaryFile *file = new QTemporaryFile();
  file->open();

  connect( reply, &QNetworkReply::readyRead, this, [reply, file, owner, projectName, fileName]()
  {
    if ( reply->error() == QNetworkReply::NoError )
    {
      file->write( reply->readAll() );
    }
  } );

  connect( reply, &QNetworkReply::finished, this, [this, reply, file, owner, projectName, fileName]()
  {
    bool failure = false;
    if ( reply->error() == QNetworkReply::NoError )
    {
      QDir dir( QStringLiteral( "%1/%2/%3/" ).arg( QFieldCloudUtils::localCloudDirectory(), owner, projectName ) );

      if ( !dir.exists() )
        dir.mkpath( QStringLiteral( "." ) );

      if ( !file->copy( dir.filePath( fileName ) ) )
        failure = true;
      file->setAutoRemove( true );
    }
    else
    {
      failure = true;
    }

    for( int i = 0; i < mCloudProjects.count(); i++ )
    {
      if ( mCloudProjects.at( i ).owner == owner && mCloudProjects.at( i ).name == projectName )
      {
        mCloudProjects[i].nbFilesDownloaded++;
        if ( failure )
          mCloudProjects[i].nbFilesFailed++;

        if ( mCloudProjects[i].nbFilesDownloaded >= mCloudProjects[i].nbFiles )
        {
          mCloudProjects[i].status = Status::Available;
          mCloudProjects[i].localPath = QStringLiteral( "%1/%2/%3" ).arg( QFieldCloudUtils::localCloudDirectory(), owner, projectName );
          QModelIndex idx = createIndex( i, 0 );
          emit dataChanged( idx, idx,  QVector<int>() << StatusRole << LocalPathRole );
          emit projectDownloaded( owner, projectName, mCloudProjects[i].nbFilesFailed > 0 );
        }
        break;
      }
    }

    file->deleteLater();
    reply->deleteLater();
  } );
}

QHash<int, QByteArray> QFieldCloudProjectsModel::roleNames() const
{
  QHash<int, QByteArray> roles;
  roles[IdRole] = "Id";
  roles[OwnerRole] = "Owner";
  roles[NameRole] = "Name";
  roles[DescriptionRole] = "Description";
  roles[StatusRole] = "Status";
  roles[LocalPathRole] = "LocalPath";
  return roles;
}

void QFieldCloudProjectsModel::reload( QJsonArray &remoteProjects )
{
  beginResetModel();
  mCloudProjects .clear();

  for ( const auto project : remoteProjects )
  {
    QVariantHash projectDetails = project.toObject().toVariantHash();
    CloudProject cloudProject( projectDetails.value( "id" ).toString(),
                          mCloudConnection ? mCloudConnection->username() : QString(),
                          projectDetails.value( "name" ).toString(),
                          projectDetails.value( "description" ).toString(),
                          Status::Available );

    QDir localPath( QStringLiteral( "%1/%2/%3" ).arg( QFieldCloudUtils::localCloudDirectory(), cloudProject.owner, cloudProject.name ) );
    if( localPath.exists()  )
      cloudProject.localPath = localPath.path() + QStringLiteral( "/qfield.qgs" );

    mCloudProjects << cloudProject;
  }

  QDirIterator ownerDirs( QFieldCloudUtils::localCloudDirectory(), QDir::Dirs | QDir::NoDotAndDotDot );
  while( ownerDirs.hasNext() )
  {
    ownerDirs.next();
    QDirIterator projectNameDirs( ownerDirs.filePath(), QDir::Dirs | QDir::NoDotAndDotDot );
    while( projectNameDirs.hasNext() )
    {
      projectNameDirs.next();
      bool found = false;
      for ( int i = 0; i < mCloudProjects.count(); i++ )
      {
        if ( mCloudProjects[i].owner == ownerDirs.fileName() && mCloudProjects[i].name == projectNameDirs.fileName() )
        {
          found = true;
        }
      }
      if ( !found )
      {
        CloudProject cloudProject( QString(), // No ID provided for local-only cloud project
                                   ownerDirs.fileName(),
                                   projectNameDirs.fileName(),
                                   QString(),
                                   Status::LocalOnly );
        cloudProject.localPath = QStringLiteral( "%1/%2/%3/qfield.qgs" ).arg( QFieldCloudUtils::localCloudDirectory(), cloudProject.owner, cloudProject.name );
        mCloudProjects << cloudProject;
      }
    }
  }

  endResetModel();
}

int QFieldCloudProjectsModel::rowCount( const QModelIndex &parent ) const
{
  if ( !parent.isValid() )
    return mCloudProjects.size();
  else
    return 0;
}

QVariant QFieldCloudProjectsModel::data( const QModelIndex &index, int role ) const
{
  if ( index.row() >= mCloudProjects.size() || index.row() < 0 )
    return QVariant();

  if ( role == IdRole )
    return mCloudProjects.at( index.row() ).id;
  else if ( role == OwnerRole )
    return mCloudProjects.at( index.row() ).owner;
  else if ( role == NameRole )
    return mCloudProjects.at( index.row() ).name;
  else if ( role == DescriptionRole )
    return mCloudProjects.at( index.row() ).description;
  else if ( role == StatusRole )
    return static_cast<int>( mCloudProjects.at( index.row() ).status );
  else if ( role == LocalPathRole )
    return mCloudProjects.at( index.row() ).localPath;

  return QVariant();
}
