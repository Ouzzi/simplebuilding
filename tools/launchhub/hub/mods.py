"""Multimod hub operations, validated selections and fixed integration launch tasks."""
import re
import sys
from . import paths, targets

sys.path.insert(0, str(paths.REPO / 'tools'))
import multimod

class ModsMixin:
    def mods_state(self):
        modules, dev = multimod.registries(self.repo)
        selected = multimod.selection(self.repo)
        props = targets.runner().gradle_properties()
        local_roots = multimod.local_roots(self.repo)
        def version(text):
            return re.sub(r'\$\{([^}]+)\}', lambda m: props.get(m[1], '?'), text)
        rows = []
        for kind, entries in (('modules', modules), ('devMods', dev)):
            for entry in entries:
                jars = []
                for project in entry.get('projects', {}).values():
                    jars.extend(self.repo.joinpath(*project.strip(':').split(':')).glob('build/libs/*.jar'))
                if kind == 'devMods':
                    for folder in ('integration/run-fabric-263/mods', 'mc26_3/fabric/build/devMods'):
                        jars.extend(self.repo.joinpath(folder).glob(f"*{entry['id'].replace('_','-')}*.jar"))
                    for source in entry['sources'].values():
                        if source.get('local'):
                            for root in local_roots: jars.extend(root.glob(source['local']))
                rows.append(dict(entry, kind=kind, enabled=entry['id'] in selected[kind],
                                 version=version(entry['version']), jarPresent=bool(jars)))
        preset_path = self.repo / 'integration/presets.json'
        custom = multimod.read(preset_path) if preset_path.exists() else {}
        builtins = {
            'only_simplebuilding': {'schemaVersion':1, 'modules':['simplebuilding'], 'devMods':['cloth_config']},
            'all': {'schemaVersion':1, 'modules':[m['id'] for m in modules], 'devMods':['cloth_config']},
            'all_dev': {'schemaVersion':1, 'modules':[m['id'] for m in modules], 'devMods':[m['id'] for m in dev]},
        }
        return {'rows':rows, 'selection':selected, 'presets':builtins | custom}

    def save_mods(self, body):
        from .api import HubError
        if self.manager.active_where(target='integration-263'):
            raise HubError(409, 'Stop the integration instance before changing its mod set.')
        value = multimod.validate_selection(body.get('selection'), self.repo)
        preset = body.get('preset')
        if preset is not None and (not isinstance(preset, str) or not re.fullmatch(r'[a-z][a-z0-9_]{1,40}', preset)
                                   or preset in ('only_simplebuilding','all','all_dev')):
            raise HubError(400, 'Invalid or reserved preset name')
        multimod.write(self.repo / 'integration/enabled-mods.json', value)
        if preset:
            path = self.repo / 'integration/presets.json'
            custom = multimod.read(path) if path.exists() else {}
            custom[preset] = value
            multimod.write(path, custom)
        return self.mods_state()

    def selection_step(self, ws):
        value = multimod.selection(self.repo)
        def copy(log):
            multimod.validate_selection(value, ws)
            multimod.write(ws / 'integration/enabled-mods.json', value)
            log('[hub] copied validated mod selection into launch workspace')
            return True
        return {'label':'apply mod selection', 'call':copy}

    def launch_integration(self, body):
        from .api import HubError
        action = body.get('action')
        tasks = {'client':'runIntegrationClient', 'server':'runIntegrationServer',
                 'fresh':'runIntegrationClient', 'tests':'runIntegrationGameTest'}
        if action not in tasks:
            raise HubError(400, 'Unknown integration action')
        selected = multimod.selection(self.repo)
        if 'simplebuilding' in selected['modules'] and 'cloth_config' not in selected['devMods']:
            raise HubError(400, 'SimpleBuilding requires Cloth Config. Enable it or disable SimpleBuilding.')
        if action == 'tests' and not {'simplebuilding','wiringexample'} <= set(selected['modules']):
            raise HubError(400, 'Integration test requires SimpleBuilding and Wiring Example.')
        if self.manager.active_where(target='integration-263'):
            raise HubError(409, 'Integration directory is already in use')
        if action in ('server', 'tests') and self.manager.active_where(kind_of='server'):
            raise HubError(409, 'Stop the running server before starting integration (shared default port).')
        workspace = body.get('workspace', 'repo')
        ws = self.workspace(workspace)
        self.require_disk(self.repo, ws)
        steps = self._gate_step(workspace) + [self.selection_step(ws)]
        if action == 'fresh':
            world = ws / 'integration/run-fabric-263/saves/Integration'
            steps.append({'label':'archive integration world',
                          'call':lambda log: self._prepare_world(world, 'recreate', log, 'Integration')})
        if action == 'tests':
            argv = targets.test_argv(ws, ['integration-263'], None)
            env = targets.test_env(['integration-263'])
        else:
            argv = [targets.gradlew_path(ws), *targets.gradle_args(), ':integration:' + tasks[action]]
            env = {}
        steps.append({'label':'integration ' + action, 'argv':argv, 'cwd':str(ws), 'env':env})
        if action == 'tests':
            modules, _ = multimod.registries(self.repo)
            module_targets = [target for module in modules
                              if module['id'] in selected['modules']
                              for target in [f"module-{module['id']}-{loader}-263" for loader in module.get('tests', {}).get('loaders', {})]]
            if module_targets:
                # Keep the harness and module runs sequential: they can share a run
                # directory, and each suite needs its own namespace filter.
                steps.append({'label':'selected module integration tests',
                              'argv':targets.test_argv(ws, module_targets, None), 'cwd':str(ws),
                              'env':targets.test_env(module_targets)})
        job = self.manager.start('test' if action == 'tests' else 'launch', 'Integration ' + action, steps,
                                 meta={'target':'integration-263', 'workspace':workspace, 'action':action,
                                       'kind_of':'server' if action == 'server' else 'client',
                                       'runDir':'integration/run-fabric-263'}, keep_stdin=action=='server')
        return {'job':job.to_dict(), 'warnings':self._dry_note([])}
