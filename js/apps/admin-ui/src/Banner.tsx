import { Banner, Flex, FlexItem } from "@patternfly/react-core";
import { ExclamationTriangleIcon } from "@patternfly/react-icons";

const WarnBanner = (text: string) => {
  return (
    <Banner screenReaderText={text} variant="gold" isSticky>
      <Flex spaceItems={{ default: "spaceItemsSm" }}>
        <FlexItem>
          <ExclamationTriangleIcon />
        </FlexItem>
        <FlexItem>{text}</FlexItem>
      </Flex>
    </Banner>
  );
};

export const Banners = () => {
  return WarnBanner("This is a warning");
};
