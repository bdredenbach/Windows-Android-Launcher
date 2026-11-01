"""Read-only integration probes. Provider outages are reported separately from native UI checks."""
import json
from pathlib import Path
import urllib.request
import xml.etree.ElementTree as ET

agent = 'NthDesktop/0.8.0 (https://github.com/bdredenbach/Windows-Android-Launcher)'
checks = [
    ('city_search', 'https://photon.komoot.io/api/?q=Portland%20Maine&limit=2&lang=en', 400000),
    ('forecast', 'https://api.met.no/weatherapi/locationforecast/2.0/compact?lat=43.661&lon=-70.255', 2000000),
    ('nasa_feed', 'https://www.nasa.gov/feed/', 512000),
]
results = []
for name, address, limit in checks:
    try:
        request = urllib.request.Request(address, headers={'User-Agent': agent})
        with urllib.request.urlopen(request, timeout=10) as response:
            data = response.read(limit + 1)
            assert len(data) <= limit, 'Response exceeds the app limit'
            status = response.status
        if name == 'city_search':
            count = len(json.loads(data)['features'])
            assert count > 0, 'No place matches'
        elif name == 'forecast':
            points = json.loads(data)['properties']['timeseries']
            assert isinstance(points[0]['data']['instant']['details']['air_temperature'], (int, float))
            count = len(points)
        else:
            root = ET.fromstring(data)
            count = sum(1 for item in root.iter() if item.tag.split('}')[-1] in ('item', 'entry'))
            assert count > 0, 'No feed items'
        results.append({'service': name, 'status': 'passed', 'http': status, 'records': count, 'bytes': len(data)})
        print(f'PASS: live {name}: HTTP {status}, {count} records, {len(data)} bytes')
    except Exception as error:
        results.append({'service': name, 'status': 'unavailable', 'reason': str(error)[:250]})
        print(f'WARN: live {name}: {error}')
Path('public-service-checks.json').write_text(json.dumps(results, indent=2) + '\n')
