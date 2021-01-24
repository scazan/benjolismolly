
local MusicUtil = require "musicutil"
local MollyThePoly = require "benjolismolly/lib/molly_the_poly_engine"

engine.name = "BenjolisMolly"

local SCREEN_FRAMERATE = 15
local screen_dirty = true

local midi_in_device

local function note_on(id, note_num, vel)
  engine.noteOn(id, MusicUtil.note_num_to_freq(note_num), vel)
end

local function note_off(note_num)
  engine.noteOff(note_num)
end

local function note_off_all()
  engine.noteOffAll()
end

local function note_kill_all()
  engine.noteKillAll()
end

local function set_pressure(id, note_num, pressure)
  engine.pressure(id, pressure)
end

local function set_pressure_all(pressure)
  engine.pressureAll(pressure)
end

local function set_timbre(note_num, timbre)
  engine.timbre(note_num, timbre)
end

local function set_timbre_all(timbre)
  engine.timbreAll(timbre)
end

local function set_pitch_bend(id, bend_st)
  engine.pitchBend(id, MusicUtil.interval_to_ratio(bend_st))
end

local function set_pitch_bend_all(bend_st)
  engine.pitchBendAll(MusicUtil.interval_to_ratio(bend_st))
end


-- Encoder input
function enc(n, delta)
  if n == 2 then
  elseif n == 3 then
  end
end

-- Key input
function key(n, z)
  if z == 1 then
    if n == 2 then

    elseif n == 3 then

    end
  end
end

-- MIDI input
local function midi_event(data)
  local msg = midi.to_msg(data)
  local channel_param = params:get("midi_channel")

  if channel_param == 1 or (channel_param > 1 and msg.ch == channel_param - 1) then
    -- Note off
    if msg.type == "note_off" then
      note_off(msg.ch)

    -- Note on
    elseif msg.type == "note_on" then
      note_on(msg.ch, msg.note, (msg.vel / 127)/4)

    -- Key pressure
    -- elseif msg.type == "key_pressure" then
      -- set_pressure(msg.note, msg.val / 127)

    -- Channel pressure
    elseif msg.type == "key_pressure" or msg.type == "channel_pressure" then
      -- set_pressure_all(msg.val / 127)
      set_pressure(msg.ch, msg.note_num, msg.val / 127)

    -- Pitch bend
    elseif msg.type == "pitchbend" then
      local bend_st = (util.round(msg.val / 2)) / 8192 * 2 -1 -- Convert to -1 to 1
      local bend_range = params:get("bend_range")
      set_pitch_bend(msg.ch, bend_st * bend_range)

    -- CC
    elseif msg.type == "cc" then
      -- Mod wheel
      if msg.cc == 74 then
        set_timbre(msg.ch, msg.val / 127)
      end

    end

  end

end

function init()
  midi_in_device = midi.connect(1)
  midi_in_device.event = midi_event

  -- Add params
  params:add{type = "number", id = "midi_device", name = "MIDI Device", min = 1, max = 4, default = 1, action = function(value)
    midi_in_device.event = nil
    midi_in_device = midi.connect(value)
    midi_in_device.event = midi_event
  end}

  local channels = {"All"}
  for i = 1, 16 do table.insert(channels, i) end
  params:add{type = "option", id = "midi_channel", name = "MIDI Channel", options = channels}

  params:add{type = "number", id = "bend_range", name = "Pitch Bend Range", min = 1, max = 48, default = 30}

  params:add_separator()

  MollyThePoly.add_params()

  local orbit = 13.5

  local screen_refresh_metro = metro.init()
  screen_refresh_metro.event = function()
    if screen_dirty then
      screen_dirty = false
      redraw()
    end
  end

  screen_refresh_metro:start(1 / SCREEN_FRAMERATE)
end


function redraw()
  screen.clear()
  screen.aa(1)

  screen.level(15)
  screen.move(3, 58)
  screen.text("fuck fuck fuck fuck fuck")
  screen.move(3, 48)
  screen.text("fuck fuck fuck fuck fuck")
  screen.move(3, 38)
  screen.text("fuck fuck fuck fuck fuck")
  screen.move(3, 28)
  screen.text("fuck fuck fuck fuck fuck")
  screen.move(3, 18)
  screen.text("fuck fuck fuck fuck fuck")
  screen.move(3, 8)
  screen.text("fuck fuck fuck fuck fuck")
  screen.fill()

  screen.update()
end
