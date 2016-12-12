#!/usr/bin/env node
'use strict';

const bootSsr = require('../src');
bootSsr(process.cwd(), process.argv.slice(2));
