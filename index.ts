// @ts-ignore
import { NativeModules } from 'react-native';

const { RNZoomUs } = NativeModules;

// const defaultJoinMeetingOptions = {
//   // some available options
//   no_driving_mode: true,
//   no_invite: true,
//   no_meeting_end_message: true,
//   no_titlebar: true,
//   no_bottom_toolbar: true,
//   no_dial_in_via_phone: true,
//   no_dial_out_to_phone: true,
//   no_disconnect_audio: true,
//   no_share: true,
//   // invite_options : InviteOptions.INVITE_VIA_EMAIL + InviteOptions.INVITE_VIA_SMS,
//   no_audio: true,
//   no_video: true,
//   // meeting_views_options : MeetingViewsOptions.NO_BUTTON_SHARE,
//   no_meeting_error_message: true,
//   participant_id: 'participant id'
// };

// export default RNZoomUs;

export async function joinMeeting(joinMeetingParams, joinMeetingOptions) {
  return RNZoomUs.joinMeeting(joinMeetingParams, (joinMeetingOptions = {}));
}

export async function initialize(appKey, appSecret) {
  return RNZoomUs.initialize(appKey, appSecret);
}

// export const InviteOptions = {};
