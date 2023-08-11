

// import Vaadin client-router to handle client-side and server-side navigation
import { Router } from '@vaadin/router';

// import Flow module to enable navigation to Vaadin server-side views
import { Flow } from 'Frontend/generated/jar-resources/Flow.js';

const { serverSideRoutes } = new Flow({
  imports: () => import('Frontend/generated/flow/generated-flow-imports.js.js')
});

const routes = [
  // for client-side, place routes below (more info https://vaadin.com/docs/v15/flow/typescript/creating-routes.html)

  // for server-side, the next magic line sends all unmatched routes:
  ...serverSideRoutes // IMPORTANT: this must be the last entry in the array
];

// Vaadin router needs an outlet in the index.html page to display views
const router = new Router(document.querySelector('#outlet'));
router.setRoutes(routes);
(window as any).Vaadin.connectionState.connectionState='connected';