/***************************************************************************
                            platformutilities.h  -  utilities for qfield

                              -------------------
              begin                : Wed Dec 04 10:48:28 CET 2015
              copyright            : (C) 2015 by Marco Bernasocchi
              email                : marco@opengis.ch
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

#ifndef PLATFORMUTILITIES_H
#define PLATFORMUTILITIES_H

#include <QObject>
#include <qgsfield.h>
#include "picturesource.h"
#include "viewstatus.h"

class ProjectSource;

class PlatformUtilities : public QObject
{
    Q_OBJECT

    Q_PROPERTY( QString configDir READ configDir CONSTANT )
    Q_PROPERTY( QString shareDir READ shareDir CONSTANT )

  public:
    virtual ~PlatformUtilities();

    virtual QString configDir() const;
    virtual QString shareDir() const;
    virtual QString packagePath() const;
    virtual QString qgsProject() const;
    virtual QString localizedDataPaths() const;
    Q_INVOKABLE bool createDir( const QString &path, const QString &dirname ) const;
    Q_INVOKABLE bool rmFile( const QString &filename ) const;
    Q_INVOKABLE bool renameFile( const QString &filename, const QString &newname ) const;

    /**
     * Get a picture from camera and copy it to the requested prefix
     * @param prefix The project folder
     * @param pictureFilePath The path (including subfolders and name) of the file
     * @return The name of the picture or null
     */
    Q_INVOKABLE virtual PictureSource *getCameraPicture( const QString &prefix, const QString &pictureFilePath, const QString &suffix );

    /**
     * Get a picture from gallery and copy it to the requested prefix
     * @param prefix The project folder
     * @param pictureFilePath The path (including subfolders and name) of the file
     * @return The name of the picture or null
     */
    Q_INVOKABLE virtual PictureSource *getGalleryPicture( const QString &prefix, const QString &pictureFilePath );

    /**
     * Open the resource (file, image, ...) that is available under \a uri.
     * The mimetype is detected to indicate the system how the file should
     * be opened.
     */
    Q_INVOKABLE virtual ViewStatus *open( const QString &uri );

    /**
     * Returns the QVariant typeName of a \a field.
     * This is a stable identifier (compared to the provider field name).
     */
    Q_INVOKABLE QString fieldType( const QgsField &field ) const;

    /**
     * Indicates the system that we want to open a project.
     * The system shall show a suitable user interface element (like a filebrowser)
     * to let the user select a project.
     * The call returns immediately and the returned ProjectSource will notify
     * when the project has actually been chosen.
     */
    Q_INVOKABLE virtual ProjectSource *openProject();

    /**
     * Indicates the system that we want to open a DCS project.
     * The system shall show a suitable user interface element (like a filebrowser)
     * to let the user select a project.
     * The call returns immediately and the returned ProjectSource will notify
     * when the project has actually been chosen.
     */
    Q_INVOKABLE virtual ProjectSource *openDCSProject();

    /**
     * Checks for positioning (GPS etc) permissions on the device.
     * If the permissions are not given, the user will be asked to grant
     * permissions.
     * It will return true, if at least coarse permissions are granted. It will
     * ask for fine permissions if none are granted.
     */
    Q_INVOKABLE virtual bool checkPositioningPermissions() const;

    /**
     * Checks for camera permissions on the device.
     * If the permissions are not given, the user will be asked to grant
     * permissions.
     */
    Q_INVOKABLE virtual bool checkCameraPermissions() const;

    /**
     * Checks for permissions to write exeternal storage.
     * If the permissions are not given, the user will be asked to grant
     * permissions.
     */
    Q_INVOKABLE virtual bool checkWriteExternalStoragePermissions() const;

    /**
     * Sets whether the device screen is allowed to go in lock mode.
     * @param allowLock if set to FALSE, the screen will not be allowed to lock.
     */
    Q_INVOKABLE virtual void setScreenLockPermission( const bool allowLock ) { Q_UNUSED( allowLock ); }

    /**
     * Show the rate this app screen if required.
    */
    Q_INVOKABLE virtual void showRateThisApp() const {};

};
#endif // PLATFORMUTILITIES_H
