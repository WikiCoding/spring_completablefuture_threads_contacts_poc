import http from 'k6/http';
import { sleep, check } from 'k6';

export const options = {
    stages: [
        { duration: '60s', target: 100 }, // ramp up
        { duration: '240s', target: 100 }, // stable
        { duration: '60s', target: 0 }, // ramp down to users
    ]
};

export default () => {
    const res = http.get('http://host.docker.internal:8081/api/v1/contacts/tc');
    check(res, { '200': (r) => r.status === 200 });
    sleep(1); // removing to test thread exhaustion!
};

// runs with: cat loadTest.js | docker run --rm -i grafana/k6 run -