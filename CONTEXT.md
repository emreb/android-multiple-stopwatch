# MultiLaps

Timing several runners at once from the trackside, where each runner sets off at a different moment and laps are recorded by hand.

## Language

**Stopwatch**:
One independent timer for one runner, carrying its own name, colour, state and laps. There are always four.
_Avoid_: Timer, lane, runner

**Label**:
A stopwatch's name and colour. They stay with the stopwatch through every reset and undo.
_Avoid_: Identity, tag

**Lap**:
One completed circuit, measured from the previous lap press (or the start) to the next lap press. Time spent stopped is not part of a lap.
_Avoid_: Split, round

**Current lap**:
The lap in progress, not yet completed.

**Last lap**:
The most recently completed lap.

**Best lap**:
The shortest completed lap on a stopwatch since its last reset.
_Avoid_: Fastest lap, record

**Total time**:
The running time accumulated on a stopwatch since its last reset, excluding time spent stopped. It includes the current lap, so it can exceed the sum of completed laps.
_Avoid_: Elapsed, duration

**Reset**:
Returning a stopwatch to idle, discarding its laps. Name and colour are kept.
_Avoid_: Clear

**Reset all**:
Resetting every stopped stopwatch at once, possible only while none is running.
_Avoid_: Clear all, new race

**Undo**:
Taking back a reset within a few seconds of it, restoring each stopwatch exactly as it was.
_Avoid_: Restore, revert

## Stopwatch states

**Idle**:
Never started since the last reset; shows zero and has no laps.

**Running**:
Time is advancing.

**Stopped**:
Time is held and can be resumed; laps are kept.
_Avoid_: Paused
