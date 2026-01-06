const { v4: uuidv4 } = require('uuid');

function buildSeedOneOemFiveDevices() {
  const oem = {
    type: 'OEMCredential',
    id: uuidv4(),
    payload: { oemId: 'oem-1', name: 'OEM ThermoGrid GmbH', status: 'active' },
  };

  const devices = [
    {
      type: 'DeviceSpecificationsCredential',
      id: uuidv4(),
      payload: {
        deviceId: 'dev-1',
        deviceName: 'Industrial Boiler A (Hall 1)',
        oemId: 'oem-1',
        gridConnectionArea: 'area1',
        ratedPowerKW: 120,
        availableFlexKW: 6,
        maxFlexCapKW: 4,
        minFlexCapKW: 0,
        deviceType: 'Boiler',
        location: 'Regensburg',
      },
    },
    {
      type: 'DeviceSpecificationsCredential',
      id: uuidv4(),
      payload: {
        deviceId: 'dev-2',
        deviceName: 'EV Charger Cluster (Parking Deck)',
        oemId: 'oem-1',
        gridConnectionArea: 'area1',
        ratedPowerKW: 80,
        availableFlexKW: 5,
        maxFlexCapKW: 3,
        minFlexCapKW: 0,
        deviceType: 'EV-Charger',
        location: 'Regensburg',
      },
    },
    {
      type: 'DeviceSpecificationsCredential',
      id: uuidv4(),
      payload: {
        deviceId: 'dev-3',
        deviceName: 'Heat Pump Array (Warehouse)',
        oemId: 'oem-1',
        gridConnectionArea: 'area2',
        ratedPowerKW: 60,
        availableFlexKW: 4,
        maxFlexCapKW: 2,
        minFlexCapKW: 0,
        deviceType: 'HeatPump',
        location: 'Regensburg',
      },
    },
    {
      type: 'DeviceSpecificationsCredential',
      id: uuidv4(),
      payload: {
        deviceId: 'dev-4',
        deviceName: 'Battery Storage (BESS) Container',
        oemId: 'oem-1',
        gridConnectionArea: 'area2',
        ratedPowerKW: 100,
        availableFlexKW: 10,
        maxFlexCapKW: 1,
        minFlexCapKW: 0,
        deviceType: 'Battery',
        location: 'Regensburg',
      },
    },
    {
      type: 'DeviceSpecificationsCredential',
      id: uuidv4(),
      payload: {
        deviceId: 'dev-5',
        deviceName: 'Backup Generator (Legacy)',
        oemId: 'oem-1',
        gridConnectionArea: 'area3',
        ratedPowerKW: 200,
        availableFlexKW: 8,
        maxFlexCapKW: 5,
        minFlexCapKW: 0,
        deviceType: 'Generator',
        location: 'Regensburg',
      },
    },
  ];

  return { oem, devices };
}

module.exports = { buildSeedOneOemFiveDevices };
