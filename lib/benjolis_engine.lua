-- Benjolis
--
-- a norns version of 
-- Alejandro Olarte's Benjolis
-- SC patch
--
-- UI:
-- use K2 and K3 to
-- cycle through pairs of dials.
--
-- use E2 and E3 to
-- adjust the left and right
-- dials of the pair.
--
-- use E1 to 
-- control volume.
--
-- if you hold K1 (shift)
-- and use K2, you will
-- have a momentary mute.
--
-- if you hold K1 (shift)
-- and use K3, you will
-- have a mute that toggles.
--
-- CONTROLS:
-- at the bottom of the params
-- menu there are four MIDI
-- mappings that you can
-- enable.
--
-- first choose the external
-- device.
--
-- then there are three options
-- for each mapping:
-- - enable mapping
-- - MIDI channel
-- - note mapping
--
-- enable mapping is, itself,
-- MIDI mappable and simply
-- enables this mapping or not.
--
-- MIDI channel sets the
-- incoming MIDI channel
-- for this mapping to 
-- listen to.
--
-- note mapping sets which
-- param this will be
-- mapped to.
--
-- thanks to Alejandro Olarte
-- for the SynthDef.
-- norns version: @scazan


local ControlSpec = require "controlspec"
local MusicUtil = require "musicutil"

local controlPairIndex = 1
local paramsInList = {}

local Benjolis = {}


-- https://stackoverflow.com/questions/11669926/is-there-a-lua-equivalent-of-scalas-map-or-cs-select-function
function map(f, t)
  local t1 = {}
  local t_len = #t
  for i = 1, t_len do
    t1[i] = f(t[i])
  end
  return t1
end

function init()
  -- IDs and short names
  paramsInList = {
    {"setFreq1", "f1", "hz", "freq 1"},
    {"setFreq2", "f2", "hz", "freq 2"},
    {"setFiltFreq", "flt", "hz", "filter freq"},
    {"setFilterType", "typ", "", "filter type"},
    {"setRungler1", "r1", "", "rungler 1 freq"},
    {"setRungler2", "r2", "", "rungler 2 freq"},
    {"setRunglerFilt", "rflt", "hz", "rungler filter"},
    {"setQ", "Q", "", "Q"},
    {"setScale", "scl", "", "scale"},
    {"setOutSignal", "out", "", "out signal"},
    {"setLoop", "loop", "", "loop"},
    {"setAmp", "vol", "", "amp"},
    {"setPan", "pan", "", "pan"},
  }

  -- add parameters from the engine
  addParams()
end

function Benjolis.addParams()
  params:add_separator()
  params:add{type = "control", controlspec = ControlSpec.new( 20.0, 14000.0, "exp", 0, 70, "Hz"), id = "setFreq1", name = "freq 1", action = engine.setFreq1}
  params:add{type = "control", controlspec = ControlSpec.new( 0.1, 14000.0, "exp", 0, 4, "Hz"), id = "setFreq2", name = "freq 2", action = engine.setFreq2}
  params:add{type = "control", controlspec = ControlSpec.new( 20.0, 20000.0, "exp", 0, 40, "Hz"), id = "setFiltFreq", name = "filter freq", action = engine.setFiltFreq}
  params:add{type = "control", controlspec = ControlSpec.new(0, 1, "lin", 1, 0, ""), id = "setFilterType", name = "filter type", action = engine.setFilterType}
  params:add_separator()

  params:add{type = "control", controlspec = ControlSpec.new( 0.0, 1.0, "lin", 0, 1), id = "setLoop", name = "loop", action = engine.setLoop}
  params:add{type = "control", controlspec = ControlSpec.new( 0.01, 9.0, "lin", 0, 1), id = "setRunglerFilt", name = "rungler filter freq", action = engine.setRunglerFilt}
  params:add{type = "control", controlspec = ControlSpec.new( 0.001, 1.0, "lin", 0, 0.02), id = "setQ", name = "Q", action = engine.setQ}
  params:add_separator()

  params:add{type = "control", controlspec = ControlSpec.new( 0.001, 1.0, "lin", 0, 0.16), id = "setRungler1", name = "rungler 1 freq", action = engine.setRungler1}
  params:add{type = "control", controlspec = ControlSpec.new( 0.001, 1.0, "lin", 0, 0.001), id = "setRungler2", name = "rungler 2 freq", action = engine.setRungler2}
  params:add{type = "control", controlspec = ControlSpec.new( 0.0, 1.0, "lin", 0, 1), id = "setScale", name = "scale", action = engine.setScale}
  params:add_separator()

  params:add{type = "control", controlspec = ControlSpec.new( 0.0, 6.0, "lin", 1, 6), id = "setOutSignal", name = "out signal", action = engine.setOutSignal}
  params:add{type = "control", controlspec = ControlSpec.new( 0.0, 6.0, "lin", 1, 6), id = "setGain", name = "gain", action = engine.setGain}
  -- params:add{type = "control", controlspec = ControlSpec.new( 0.0, 1.0, "lin", 0, 0), id = "setAmp", name = "amp", action = engine.setAmp}
  params:add{type = "control", controlspec = ControlSpec.AMP, id = "setAmp", name = "amp", action = engine.setAmp}
  params:add{type = "control", controlspec = ControlSpec.new( -1, 1, "lin", 0.001, 0), id = "setPan", name = "pan", action = engine.setPan}
  params:add_separator()
end

return Benjolis
