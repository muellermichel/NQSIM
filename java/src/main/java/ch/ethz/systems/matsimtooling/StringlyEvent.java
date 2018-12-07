package ch.ethz.systems.matsimtooling;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class StringlyEvent {

    public static boolean isEquivalent(String one, String two) {
        if (one == null && two == null) {
            return true;
        }
        if (one == null || two == null) {
            return false;
        }
        return one.equals(two);
    }

    @JacksonXmlProperty(isAttribute = true)
    public String time;

    @JacksonXmlProperty(isAttribute = true)
    public String type;

    @JacksonXmlProperty(isAttribute = true)
    public String person;

    @JacksonXmlProperty(isAttribute = true)
    public String link;

    @JacksonXmlProperty(isAttribute = true)
    public String actType;

    @JacksonXmlProperty(isAttribute = true)
    public String legMode;

    @JacksonXmlProperty(isAttribute = true)
    public String vehicle;

    @JacksonXmlProperty(isAttribute = true)
    public String facility;

    @JacksonXmlProperty(isAttribute = true)
    public String delay;

    @JacksonXmlProperty(isAttribute = true)
    public String networkMode;

    @JacksonXmlProperty(isAttribute = true)
    public String relativePosition;

    @JacksonXmlProperty(isAttribute = true)
    public String distance;

    @JacksonXmlProperty(isAttribute = true)
    public String driverId;

    @JacksonXmlProperty(isAttribute = true)
    public String vehicleId;

    @JacksonXmlProperty(isAttribute = true)
    public String transitLineId;

    @JacksonXmlProperty(isAttribute = true)
    public String transitRouteId;

    @JacksonXmlProperty(isAttribute = true)
    public String departureId;

    @JacksonXmlProperty(isAttribute = true)
    public String agent;

    @JacksonXmlProperty(isAttribute = true)
    public String atStop;

    @JacksonXmlProperty(isAttribute = true)
    public String destinationStop;

    public boolean equals(StringlyEvent ref, boolean exactTimeRequired) {
        if (exactTimeRequired && !isEquivalent(time, ref.time)) {
            return false;
        }
        return isEquivalent(type, ref.type)
            && isEquivalent(person, ref.person)
            && isEquivalent(link, ref.link)
            && isEquivalent(actType, ref.actType)
            && isEquivalent(legMode, ref.legMode)
            && isEquivalent(vehicle, ref.vehicle)
            && isEquivalent(facility, ref.facility)
            && isEquivalent(delay, ref.delay)
            && isEquivalent(networkMode, ref.networkMode)
            && isEquivalent(relativePosition, ref.relativePosition)
            && isEquivalent(distance, ref.distance)
            && isEquivalent(driverId, ref.driverId)
            && isEquivalent(vehicleId, ref.vehicleId)
            && isEquivalent(transitLineId, ref.transitLineId)
            && isEquivalent(transitRouteId, ref.transitRouteId)
            && isEquivalent(departureId, ref.departureId)
            && isEquivalent(agent, ref.agent)
            && isEquivalent(atStop, ref.atStop)
            && isEquivalent(destinationStop, ref.destinationStop);
    }

    public String toString() {
        if (transitLineId == null) {
            if (facility == null) {
                if (agent == null) {
                    return String.format(
                            "t=%s:type=%s:p=%s:l=%s(%s,%s,%s,%s,%s,%s)",
                            time,
                            type,
                            person,
                            link,
                            actType,
                            legMode,
                            vehicle,
                            networkMode,
                            relativePosition,
                            distance
                    );
                }
                return String.format(
                        "t=%s:type=%s:agent=%s:atStop=%s:destStop=%s",
                        time,
                        type,
                        agent,
                        atStop,
                        destinationStop
                );
            }
            return String.format(
                    "t=%s:type=%s:v=%s:f=%s:delay=%s",
                    time,
                    type,
                    vehicle,
                    facility,
                    delay
            );
        }
        return String.format(
                "t=%s:type=%s:d=%s:v=%s:line=%s:route=%s:departure=%s",
                time,
                type,
                driverId,
                vehicleId,
                transitLineId,
                transitRouteId,
                departureId
        );

    }
}
