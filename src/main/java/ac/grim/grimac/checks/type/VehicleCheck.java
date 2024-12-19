package ac.grim.grimac.checks.type;

import ac.grim.grimac.api.AbstractCheck;
import ac.grim.grimac.api.CheckType;
import ac.grim.grimac.utils.anticheat.update.VehiclePositionUpdate;

public interface VehicleCheck extends AbstractCheck {

    void process(final VehiclePositionUpdate vehicleUpdate);

    @Override
    default int getCheckMask() {
        return CheckType.VEHICLE.getMask();
    }
}
